import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DependecyRelationship {
	public static String[] words = new String[] { "we", "you", "they", "her",
			"his", "their", "them", "it", "he", "she", "him", "your", "me",
			"I", "my", "mine", "us", "yours", "hers", "theirs" };

	public static void main(String[] args) {

	}

	public static List<Pair<String, Pair<String, String>>> Dependencies(
			String Sentence, Map<Pair<Integer, Integer>, String> m)
			throws Exception {

		List<Pair<String, Pair<String, String>>> answer = new ArrayList<Pair<String, Pair<String, String>>>();
		Annotation Document = new Annotation(Sentence);
		//annotating the document.
		processMain.pipeline.annotate(Document);
		List<CoreMap> sentence = Document.get(SentencesAnnotation.class);

		//for storing the question and document dependencies for furthur use.
		String fileName = "q_dependencies.txt";
		String fileName2 = "q_pronoun_resolution.txt";
		if (processMain.should) {
			fileName = "doc_dependencies.txt";
			fileName2 = "doc_pronoun_resolution.txt";
		}
		PrintWriter file = new PrintWriter(fileName, "utf-8");
		PrintWriter file2 = new PrintWriter(fileName2, "utf-8");

		//for each sentence in the document
		for (CoreMap s : sentence) {
			file.println("");
			processMain.numberOfSentence += 1;

			//get the basic Dependencies from a sentence.
			SemanticGraph dependencies1 = s
					.get(BasicDependenciesAnnotation.class);
			List<SemanticGraphEdge> edge_set1 = dependencies1.edgeListSorted();

			//for all semantic graph edge
			for (SemanticGraphEdge e : edge_set1) {
				IndexedWord gov = e.getGovernor();
				IndexedWord dep = e.getDependent();

				//first lemmatized the entities.
				String firstWord = gov.get(LemmaAnnotation.class);
				String secondWord = dep.get(LemmaAnnotation.class);


				//Now first check their POS TAG if it is Pronoun then replace it with it's represantative mention.
				String t1 = e.getGovernor().get(PartOfSpeechAnnotation.class);
				String t2 = e.getDependent().get(PartOfSpeechAnnotation.class);
				if (processMain.should == true && (t1.compareTo("PRP$") == 0 || t1.compareTo("PRP") == 0)) {
					Pair<Integer, Integer> t = new Pair<Integer, Integer>(e
							.getGovernor().sentIndex(),
							e.getGovernor().index() - 1);

					if (m.containsKey(t)) {
						Boolean flag = false;

						for (String s1 : words) {
							if (s1.compareTo(m.get(t).toLowerCase()) == 0) {
								flag = true;
							}
						}

						if (flag == false) {
							file2.println(t.first.toString() + " "
									+ t.second.toString() + " " + m.get(t));
							firstWord = m.get(t);
						}
					}

				}

				if (processMain.should == true && (t2.compareTo("PRP$") == 0 || t2.compareTo("PRP") == 0)) {
					Pair<Integer, Integer> t = new Pair<Integer, Integer>(e
							.getDependent().sentIndex(), e.getDependent()
							.index() - 1);
					if (m.containsKey(t)) {
						Boolean flag = false;

						for (String s1 : words) {
							if (s1.compareTo(m.get(t).toLowerCase()) == 0) {
								flag = true;
							}
						}

						if (flag == false) {
							file2.println(t.first.toString() + " "
									+ t.second.toString() + " " + m.get(t));
							secondWord = m.get(t);
						}
					}
				}


				//getting the grammatical realtion.
				GrammaticalRelation r = e.getRelation();
				answer.add(new Pair<String, Pair<String, String>>(r.toString(),
						new Pair<String, String>(firstWord, secondWord)));
				Pair<String, Pair<String, String>> p = answer
						.get(answer.size() - 1);

				//getting the first second and relation extract by removing the extra symbols.
				String rel = p.first.replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
				String first = p.second.first.replaceAll("[^a-zA-Z0-9]", "");
				String second = p.second.second.replaceAll("[^a-zA-Z0-9]", "");


				//storing these dependencies to a file.
				if (first.compareTo("") != 0 && second.compareTo("") != 0)
					file.println(first + " " + rel + " " + second);


				//storing these dependencies to map with reference to their statement index.
				//doing this for Filter.
				if (processMain.should) {

					Pair<String, Pair<String, String>> p2 = new Pair<String, Pair<String, String>>(
							rel, new Pair<String, String>(first, second));
					Pair<String, Pair<String, String>> p3 = new Pair<String, Pair<String, String>>(
							rel, new Pair<String, String>(second, first));

					if (processMain.DependencySentence.containsKey(p2)) {
						List<Integer> s1 = processMain.DependencySentence
								.get(p2);
						s1.add(e.getGovernor().sentIndex());
						processMain.DependencySentence.put(p2, s1);
						processMain.DependencySentence.put(p3, s1);
					} else {
						List<Integer> s1 = new ArrayList<Integer>();
						s1.add(e.getGovernor().sentIndex());
						processMain.DependencySentence.put(p2, s1);
						processMain.DependencySentence.put(p3, s1);
					}
				}
			}
		}
		file.close();
		file2.close();
		return answer;
	}
}
