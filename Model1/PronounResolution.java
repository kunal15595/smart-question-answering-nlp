
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PronounResolution {
  public static void main(String[] args){
	  
  }
  public static Map<Pair<Integer,Integer>,String> ResolvePronoun(String Text) throws Exception {
  	  //document text.
	  String s1 = Text;
	  //annotating document.
	  Annotation Document = new Annotation(s1);
	  processMain.pipeline.annotate(Document);

	  //This is the coreference Chain obtained from the text.
	  Map<Integer,CorefChain> graph = Document.get(CorefChainAnnotation.class);

	  Map<Pair<Integer,Integer>,String> answer = new HashMap<Pair<Integer,Integer>,String>();

	  //for all coref chains extract the representative mention and their corresponding references.
	  for(Map.Entry<Integer, CorefChain> entry: graph.entrySet()){
		  CorefChain c = entry.getValue();
		  if(c.getMentionsInTextualOrder().size()<=1){
			  continue;
		  }
		  //representative mention.
		  CorefMention cm = c.getRepresentativeMention();
		  String clust = "";

		  //all references for the representative.
		  List<CoreLabel> tks = Document.get(SentencesAnnotation.class).get(cm.sentNum-1).get(TokensAnnotation.class);
		  for(int i = cm.startIndex-1; i < cm.endIndex-1; i++)
                clust += tks.get(i).get(TextAnnotation.class) + " ";
            clust = clust.trim();
            for(CorefMention m : c.getMentionsInTextualOrder()){
            	//putting all the tupples in the Map.
                answer.put(new Pair<Integer,Integer>(m.sentNum-1,m.startIndex-1), clust);
            }
	  }
	  return answer;
  }	  
}


