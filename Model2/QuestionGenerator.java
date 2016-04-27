//This is the code for the Key Word based Model
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.Lesk;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.stanford.nlp.hcoref.CorefCoreAnnotations.CorefChainAnnotation;

import edu.stanford.nlp.hcoref.data.CorefChain;
import edu.stanford.nlp.hcoref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.Pair;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class QuestionGenerator {
        ILexicalDatabase db;
        Properties p;
        StanfordCoreNLP pipeline;
        Map<String,String> questionReplace;
            
        QuestionGenerator(){
            questionReplace = new HashMap<String,String>();
            db = new NictWordNet();
            p = new Properties();
            p.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse,dcoref");
            p.put("dcoref.score",true);
            pipeline = new StanfordCoreNLP(p);
        }
	
	//This is the function that gives the answer to the question
	public List<Pair<String,String>> questionTagger(String question, String file){
		String input = "";
		try (BufferedReader br = new BufferedReader(new FileReader(file))){
                    String currentLine;
                    while ((currentLine = br.readLine()) != null) {
                        if(!currentLine.isEmpty()){
                            input = input.concat(currentLine);
                            input = input.concat(" ");
                        }
                    }
		} catch (IOException e) {
                    e.printStackTrace();
		} 
                List<String> pronounResolvedInput = coreferenceResolution(input, question); //Perform Coreference Resolution on the document
                List<String> inputList = sentenceSplitter(pronounResolvedInput.get(0)); //Split the document into sentences
                List<String> oldInputList = sentenceSplitter(input);
		List<String> questionList = sentenceSplitter(question);
		Map<String, Double> questionKeywords = keywordsExtractor(questionList,1).get(0); //Get question keywords
                List<Map<String, Double>> inputKeywords = keywordsExtractor(inputList,0);	//Get keywords for each sentence
                Map<Integer, Float> percentMatch = new HashMap<Integer, Float>();
                int i = 0;
                HashMap<Integer,Double> matchMap = new HashMap<Integer,Double>();
                for (Map<String, Double> sentenceKey: inputKeywords){
                    double match = matcher(sentenceKey, questionKeywords); //Find the most relevant sentence to the question
                    matchMap.put(i, match);
                    i += 1;
                }
                HashMap<Integer, Double> sortedMap = sortHashMapByValues(matchMap);
                List<String> bestSentenceArr = new ArrayList<String>();
                List<Integer> bestSentenceIndexArr = new ArrayList<Integer>();
                double bestCount = -1;
                System.out.println("");
                System.out.println("Sentence Priority Order");
                for (Map.Entry<Integer, Double> entry : sortedMap.entrySet()) {
                    System.out.println(inputList.get(entry.getKey())  + " : " + entry.getValue());
                    if(bestCount == -1){
                        bestCount = entry.getValue();
                    }
                    if(entry.getValue() == bestCount){
                        bestSentenceArr.add(inputList.get(entry.getKey()));
                        bestSentenceIndexArr.add(entry.getKey());
                    }                        
                }
                System.out.println("");
                int k =0;
                Annotation questionDoc = new Annotation(question);
                List<String> relationships = new ArrayList<String>();
                pipeline.annotate(questionDoc);
                
                for (CoreMap sentence : questionDoc.get(CoreAnnotations.SentencesAnnotation.class)) {
                        SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                        List<IndexedWord> questionNodes = dependencies.getAllNodesByPartOfSpeechPattern("WP|WRB|WDT");
                        List<String> answerNodes = new ArrayList<String>();
                        if(questionNodes.size() > 0){
                            List<SemanticGraphEdge> edge_set1 = dependencies.edgeListSorted();
                            int j=0;
                            for(SemanticGraphEdge edge : edge_set1){
                                j++;
                                Iterator<SemanticGraphEdge> it = edge_set1.iterator();
                                IndexedWord dep = edge.getDependent();
                                String dependent = dep.word();
                                int dependent_index = dep.index();
                                IndexedWord gov = edge.getGovernor();
                                String governor = gov.word();
                                int governor_index = gov.index();
                                GrammaticalRelation relation = edge.getRelation();
                                String relationStringrelation = relation.toString().replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
                                if(questionNodes.contains(dep)) {
                                    relationships.add(relationStringrelation);
                                }else if(questionNodes.contains(gov)){
                                    relationships.add(relationStringrelation);
                                }
                            }
                        }
                    }
                List<Pair<String,String>> answers = new ArrayList<Pair<String,String>>();
                List<String> answerString = new ArrayList<String>();
                String[] questionTokens = question.toLowerCase().split(" ");
                for(int bestSentenceIndex : bestSentenceIndexArr){
                    int ansFound = 0;
                    List<String> answerKeyWords = new ArrayList<String>();
                    for(String word1 : questionKeywords.keySet()){
                        if(questionKeywords.get(word1) > 0.0){
                            double maxVal = 0;
                            String maxWord = null;
                            for(String word2: inputKeywords.get(bestSentenceIndex).keySet()){
                                if(!word1.equals(word2)){
                                    double sim = wordDistance(word1, word2);
                                    if(maxWord == null){
                                        maxVal = sim;
                                        maxWord = word2;
                                    }else if( maxVal < sim){
                                        maxVal = sim;
                                        maxWord = word2;
                                    }
                                   }else{
                                       maxVal = 1.0;
                                       maxWord = word2;
                                       break;
                                    }
                            }
                            answerKeyWords.add(maxWord);

                        }
                    }
                    Annotation document = new Annotation(bestSentenceArr.get(k));
                    k+=1;
                    pipeline.annotate(document);
                    for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
                        SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                        List<SemanticGraphEdge> edge_set = dependencies.edgeListSorted();
                        Map<String,String> compoundMap = new HashMap<String,String>();
                        List<String> allRelationships = new ArrayList<String>();
                        int isRelationshipPresent = 0;
                        for(SemanticGraphEdge e : edge_set){
                            IndexedWord gov = e.getGovernor();
                            IndexedWord dep = e.getDependent();
                            String firstWord = gov.get(LemmaAnnotation.class);
                            String secondWord = dep.get(LemmaAnnotation.class);
                            String firstWordNonLemma = gov.word();
                            String secondWordNonLemma = dep.word();
                            GrammaticalRelation relation = e.getRelation();
                            String relationship = relation.toString().replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
                            allRelationships.add(relationship);
                            if(answerKeyWords.contains(firstWord)){
                                if(inputKeywords.get(bestSentenceIndex).containsKey(secondWord)){
                                    if(!questionKeywords.containsKey(secondWord)){
                                        int cont = 1;
                                        for (String token : questionTokens){
                                            if(token.equals("whom") || token.equals("who") || token.equals("where")){
                                                if(!dep.get(CoreAnnotations.PartOfSpeechAnnotation.class).matches("NN|NNS|NNP|NNPS|CD")){
                                                    cont = 0;
                                                    break;
                                                }
                                            }
                                        }
                                        if(cont == 1){
                                        if(relationships.contains(relationship)){
                                            String outputString = secondWordNonLemma;                                           
                                            int toAdd = 1;                                          
                                            if(toAdd == 1){
                                                answers.add(new Pair<String,String>(oldInputList.get(bestSentenceIndex),outputString));
                                                answerString.add(outputString);
                                                ansFound= 1;
                                                isRelationshipPresent = 1;
                                            }
                                            System.out.println("Possible Answer:"+outputString);
                                        }
                                        }
                                    }
                                }
                            }
                            if(answerKeyWords.contains(secondWord)){
                                if(inputKeywords.get(bestSentenceIndex).containsKey(firstWord)){
                                    if(!questionKeywords.containsKey(firstWord)){
                                        int cont = 1;
                                        for (String token : questionTokens){
                                            if(token.equals("whom") ||  token.equals("who") || token.equals("where")){
                                                if(!gov.get(CoreAnnotations.PartOfSpeechAnnotation.class).matches("NN|NNS|NNP|NNPS|CD")){
                                                    cont = 0;
                                                    break;
                                                }
                                            }
                                        }
                                        if(cont == 1){
                                       if(relationships.contains(relationship)){
                                            String outputString = firstWordNonLemma;                                            
                                            int toAdd = 1;
                                            if(toAdd == 1){
                                                answers.add(new Pair<String,String>(oldInputList.get(bestSentenceIndex),outputString));
                                                answerString.add(outputString);
                                                ansFound= 1;
                                                isRelationshipPresent = 1;
                                            }
                                        }
                                       }
                                    }
                                }
                            }
                            if(relationship.equals("compound")){
                                if(!compoundMap.containsKey(firstWordNonLemma)){
                                    compoundMap.put(firstWordNonLemma, secondWordNonLemma);
                                }else{
                                    compoundMap.put(firstWordNonLemma, compoundMap.get(firstWordNonLemma)+" "+secondWordNonLemma);
                                }
                            }
                        }
                        if(isRelationshipPresent == 0){
                            for(SemanticGraphEdge e : edge_set){
                                IndexedWord gov = e.getGovernor();
                                IndexedWord dep = e.getDependent();
                                String firstWord = gov.get(LemmaAnnotation.class);
                                String secondWord = dep.get(LemmaAnnotation.class);
                                String firstWordNonLemma = gov.word();
                                String secondWordNonLemma = dep.word();
                                GrammaticalRelation relation = e.getRelation();
                                String relationship = relation.toString().replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
                                 
                                if(answerKeyWords.contains(firstWord)){
                                     if(inputKeywords.get(bestSentenceIndex).containsKey(secondWord)){
                                        if(!questionKeywords.containsKey(secondWord)){
                                            int cont = 1;
                                            for (String token : questionTokens){
                                                if(token.equals("whom") || token.equals("who") || token.equals("where")){
                                                    if(!dep.get(CoreAnnotations.PartOfSpeechAnnotation.class).matches("NN|NNS|NNP|NNPS|CD")){
                                                        cont = 0;
                                                        break;
                                                    }
                                                }
                                            }
                                            
                                            if(cont == 1){
                                          
                                                String outputString = secondWordNonLemma;
                                                String startString = secondWordNonLemma;
                                                while(compoundMap.containsKey(startString)){
                                                    outputString = compoundMap.get(startString)+" "+outputString;
                                                    startString = compoundMap.get(startString);

                                                }
                                                int toAdd = 1;
                                                if(toAdd == 1){
                                                    answers.add(new Pair<String,String>(oldInputList.get(bestSentenceIndex),outputString));
                                                    answerString.add(outputString);
                                                    ansFound= 1;
                                                }
                                                
                                                System.out.println("Possible Answer:"+outputString);
                                           
                                            }
                                        }
                                    }
                                }
                                if(answerKeyWords.contains(secondWord)){  
                                    if(inputKeywords.get(bestSentenceIndex).containsKey(firstWord)){
                                       if(!questionKeywords.containsKey(firstWord)){
                                            int cont = 1;
                                            for (String token : questionTokens){
                                                if(token.equals("whom") ||  token.equals("who") || token.equals("where")){
                                                    if(!gov.get(CoreAnnotations.PartOfSpeechAnnotation.class).matches("NN|NNS|NNP|NNPS|CD")){
                                                        cont = 0;
                                                        break;
                                                    }
                                                }
                                            }
                                           

                                            if(cont == 1){
                                             
                                                String outputString = firstWordNonLemma;
                                                String startString = firstWordNonLemma;
                                                while(compoundMap.containsKey(startString)){
                                                    outputString = compoundMap.get(startString)+" "+outputString;
                                                    startString = compoundMap.get(startString);
                                                }
                                                int toAdd = 1;
                                                if(toAdd == 1){
                                                    answers.add(new Pair<String,String>(oldInputList.get(bestSentenceIndex),outputString));
                                                    answerString.add(outputString);
                                                    ansFound= 1;
                                                }
                                            }
                                          
                                        }
                                    }
                                }
                            }
                        }else{
                            int p = 0;
                           
                            for(Pair<String,String> q: answers){
                                String outputString = q.second;
                                String startString = q.second;
                                while(compoundMap.containsKey(startString)){
                                    outputString = compoundMap.get(startString)+" "+outputString;
                                    startString = compoundMap.get(startString);
                                }
                                answers.set(p, new Pair<String,String>(q.first,outputString));
                                p += 1;
                            }
                        }
                    }
                    if(ansFound == 0){
                        answers.add(new Pair<String,String>(oldInputList.get(bestSentenceIndex),"<Poor Dependencies - Sentence Identified>"));                                                
                    } 
                }
            return answers;
	}
	
	//This function sorts a HashMap by Values
        public LinkedHashMap<Integer, Double> sortHashMapByValues(HashMap<Integer, Double> passedMap) {
            List<Integer> mapKeys = new ArrayList<Integer>(passedMap.keySet());
            List<Double> mapValues = new ArrayList<Double>(passedMap.values());
            Collections.sort(mapValues);
            Collections.sort(mapKeys);
            Collections.reverse(mapValues);
            Collections.reverse(mapKeys);
            LinkedHashMap<Integer, Double> sortedMap =
                new LinkedHashMap<Integer, Double>();

            Iterator<Double> valueIt = mapValues.iterator();
            while (valueIt.hasNext()) {
                double val = valueIt.next();
                Iterator<Integer> keyIt = mapKeys.iterator();

                while (keyIt.hasNext()) {
                    Integer key = keyIt.next();
                    double comp1 = passedMap.get(key);
                    double comp2 = val;

                    if (comp1 == comp2) {
                        keyIt.remove();
                        sortedMap.put(key, val);
                        break;
                    }
                }
            }
            return sortedMap;
        }
        

	//This function performs Coreference resolution on the document and returns the document with pronouns removed.
        public List<String> coreferenceResolution(String content, String question){
            questionReplace.clear();
            List<String> sentences = sentenceSplitter(content);
            Annotation document = new Annotation(content);
            pipeline.annotate(document);
            Map<Integer,CorefChain> graph = document.get(CorefChainAnnotation.class);
            Map<Pair<Integer,Integer>,String> answer = new HashMap<Pair<Integer,Integer>,String>();
            Map<Pair<Integer,Integer>,String> replaceList = new HashMap<Pair<Integer,Integer>,String>();
            for(Map.Entry<Integer, CorefChain> entry: graph.entrySet()){
                CorefChain c = entry.getValue();
                if(c.getMentionsInTextualOrder().size()<=1){
                    continue;
                }
                CorefMention cm = c.getRepresentativeMention();
                String clust = "";
                List<CoreLabel> tks = document.get(CoreAnnotations.SentencesAnnotation.class).get(cm.sentNum-1).get(CoreAnnotations.TokensAnnotation.class);
                int r = 0;
                for(int i = cm.startIndex-1; i < cm.endIndex-1; i++){
                    if(!tks.get(i).get(CoreAnnotations.PartOfSpeechAnnotation.class).matches("PRP|PRP\\$")){
                        r = 1;
                    }
                    clust += tks.get(i).get(CoreAnnotations.TextAnnotation.class) + " ";
                }
                clust = clust.trim();
                for(CorefMention m : c.getMentionsInTextualOrder()){
                    String token = document.get(CoreAnnotations.SentencesAnnotation.class).get(m.sentNum-1).get(CoreAnnotations.TokensAnnotation.class).get(m.startIndex-1).get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    int index = document.get(CoreAnnotations.SentencesAnnotation.class).get(m.sentNum-1).get(CoreAnnotations.TokensAnnotation.class).get(m.startIndex-1).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
                    int index1 = document.get(CoreAnnotations.SentencesAnnotation.class).get(m.sentNum-1).get(CoreAnnotations.TokensAnnotation.class).get(m.startIndex-1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
                    questionReplace.put(token,clust);
                    if(token.matches("PRP|PRP\\$") && r == 1){                        
                        replaceList.put(new Pair<Integer,Integer>(index,index1), clust);
                        answer.put(new Pair<Integer,Integer>(m.sentNum-1,m.startIndex-1), clust);
                    }
                }
            }
            SortedSet<Pair<Integer,Integer>> keys = new TreeSet<Pair<Integer,Integer>>(replaceList.keySet());
            List<Pair<Integer,Integer>> keySet = new ArrayList<Pair<Integer,Integer>>();
            for (Pair<Integer,Integer> key : keys) { 
               keySet.add(key);
         
            }
            Collections.reverse(keySet);
            for (Pair<Integer,Integer> key : keySet){
                content = content.substring(0,key.first) + replaceList.get(key) + content.substring(key.second);
            }
            List<String> returnList = new ArrayList<String>();
            returnList.add(content);
            returnList.add(question);
            return returnList;
        }

        //This function gives the similarity score between sentence keywords and question keywords      
        public double matcher(Map<String, Double> sentenceKey , Map<String, Double>  questionKeywords){
            double count = 0;
            double sum = 0;
            for(String word1 : questionKeywords.keySet()){
                double maxVal = 0;
                String maxWord = null;
                for(String word2: sentenceKey.keySet()){
                   double mult = 1.0;
                  if(questionKeywords.get(word1) > 0.0){
                    mult = 2.0;
                   }
                  if(!word1.equals(word2)){
                    double sim = wordDistance(word1, word2)*mult;
                    sum += sim;
                    if(maxWord == null){
                        maxVal = sim;
                        maxWord = word2;
                    }else if( maxVal < sim){
                        maxVal = sim;
                        maxWord = word2;
                    }
                   }else{
                       maxVal = 1.0*mult;
                       maxWord = word2;
                       sum += 1.0*mult;
                       break;
                   }
                }
                count += maxVal;
            }
            
            return count;
        }
        
	//This function gives the similarity score between two words
        public double wordDistance(String word1, String word2){
		WS4JConfiguration.getInstance().setMFS(true);
		double s = new Path(db).calcRelatednessOfWords(word1, word2);
		return s;
        }
        
	//This function provides the keywords for an input sentence.
	public List<Map<String, Double>> keywordsExtractor(List<String> sentenceList, Integer isQuestion){            
            List<Map<String, Double>> words = new ArrayList<Map<String, Double>>();
            for (String content : sentenceList){
                Annotation document = new Annotation(content);
                pipeline.annotate(document);
                Set<String> temp = new HashSet<String>();
                Map<String, Double> keywords = new HashMap<String, Double>();
                for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
                    SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
                    List<IndexedWord> questionNodes = dependencies.getAllNodesByPartOfSpeechPattern("WP|WRB|WDT|WRB|WP\\$");
                    List<String> answerNodes = new ArrayList<String>();
                    if(questionNodes.size() > 0 && isQuestion == 1){
                        List<SemanticGraphEdge> edge_set1 = dependencies.edgeListSorted();
                        int j=0;
                        for(SemanticGraphEdge edge : edge_set1){
                            j++;
                            Iterator<SemanticGraphEdge> it = edge_set1.iterator();
                            IndexedWord dep = edge.getDependent();
                            String dependent = dep.word();
                            int dependent_index = dep.index();
                            IndexedWord gov = edge.getGovernor();
                            String governor = gov.word();
                            int governor_index = gov.index();
                            GrammaticalRelation relation = edge.getRelation();
                            String relationStringrelation = relation.toString().replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
                            if(questionNodes.contains(dep)) {
                                answerNodes.add(gov.word());
                            }else if(questionNodes.contains(gov)){
                                answerNodes.add(dep.word()); //Identify the most important word in the question
                            }
                        }
                    }
                    for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {			    	
                        String word = token.get(CoreAnnotations.TextAnnotation.class);
                        String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                        String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                        String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                        if(!ne.equals("O") || pos.matches("JJ|JJR|JJS|RB|RBR|RBS|NN|NNS|NNP|NNPS|VB|VBD|VBG|VBN|VBP|VBZ|CD") || answerNodes.contains(word)){
                                List<String> tuple = new ArrayList<String>();
                                tuple.add(pos);
                                tuple.add(ne);
                                if(answerNodes.contains(word)){
                                    keywords.put(lemma, 1.0);
                                }else{
                                    keywords.put(lemma, 0.0);
                                }
                                temp.add(lemma);
                        }	
                    }
                }
                words.add(keywords);
            }
            return words;
	}
	
	//This function splits a document into sentences.
        public List<String> sentenceSplitter(String input){
		Reader reader = new StringReader(input);
		DocumentPreprocessor dp = new DocumentPreprocessor(reader);
		List<String> sentenceList = new ArrayList<String>();

		for (List<HasWord> sentence : dp) {
		   String sentenceString = Sentence.listToString(sentence);
		   sentenceList.add(sentenceString.toString());
		}
		return sentenceList;
		
	}
	
	//Main function
        public static void main(String[] args) throws IOException{
		QuestionGenerator t = new QuestionGenerator();
                String question = args[0];
               String input = args[1];
               String output = args[2];
               List<String> questionsArr = new ArrayList<String>();
               List<String> inputArr = new ArrayList<String>();
               BufferedReader br = new BufferedReader(new FileReader(question));
               String line;
               while((line = br.readLine())!= null){
                   questionsArr.add(line.replaceAll("(\\r|\\n)", ""));
               }
               br.close();
               br = new BufferedReader(new FileReader(input));
              
               while((line = br.readLine())!= null){
                   inputArr.add(line.replaceAll("(\\r|\\n)", ""));
               }
               br.close();
               
               int k = 0;
               for(String ques : questionsArr){
                    List<Pair<String,String>> answer = t.questionTagger(ques, inputArr.get(k));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(output,true));
                    System.out.println("");
                    for(Pair<String,String> a : answer){
                        System.out.println("Final Answer:"+a.second);
                        System.out.println("Line:"+a.first);
                        System.out.println("");
                        bw.write(ques+"? : "+a.second+"\nLine: "+a.first+"\n"+"\n");
                    }
                    bw.close();
                    k += 1;
               }
        }
}
