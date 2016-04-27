import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.Pair;


//this file contains contains the main module for QA System.

public class processMain {
	public static Properties props;
	public static StanfordCoreNLP pipeline;
	public static String answerDep;
	public static List<String> notUseFull = new ArrayList<String>();
	public static Map<Pair<String,Pair<String,String>>, List<Integer>> DependencySentence;
	public static Boolean should;
	public static List<Pair<String,Pair<String,String>>> tobeCheked;
	public static int numberOfSentence=0;
	public static List<Pair<String,Pair<String,String>>> tobeDone;
	

	//for finding the intersection between two list. 
	public static List<Integer> intersection2(List<Integer> a, List<Integer> b){
		List<Integer> temp = new ArrayList<Integer>();
		for(Integer t1 : a){
			for(Integer t2 : b){
				if(t1.compareTo(t2)==0){
					temp.add(t1);
				}
			}
		}
		return temp;
	}
	
	//for finding whether the true false queries dependencies are from a single statement. A Filter.
	public static Boolean interseciton2(){
		List<Integer> temp = new ArrayList<Integer>();
		for(int i=0;i<numberOfSentence+1;i++){
			temp.add(i);
		}
		for(Pair<String,Pair<String,String>> L : tobeCheked){
			if(DependencySentence.containsKey(L)){
				temp = intersection2(temp,DependencySentence.get(L));
			}
			else {
				return false;
			}
		}
		if(temp.size()==0){
			return false;
		}
		else {
			return true;
		}
	}

	//for finding whether the Single answer query's dependencies are from a single statement. A Filter.
	public static List<String> intersection(List<String> possible){
		List<Integer> temp = new ArrayList<Integer>();
		for(int i=0;i<numberOfSentence+1;i++){
			temp.add(i);
		}
		List<Pair<String,Pair<String,String>>> L1 = new ArrayList<Pair<String,Pair<String,String>>>();
		for(Pair<String,Pair<String,String>> L : tobeCheked){
			String rel = L.first.replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
	    	String first = L.second.first.replaceAll("[^a-zA-Z0-9]", "");
	    	String second = L.second.second.replaceAll("[^a-zA-Z0-9]", "");
	    		if(first.startsWith("wh") || second.startsWith("wh")){
	    			L1.add(L);
					continue;
			}
			if(DependencySentence.containsKey(L)){
				temp = intersection2(temp,DependencySentence.get(L));
			}
		}
		
		List<String> answer= new ArrayList<String>();
		
		for(String ans : possible){
			List<Integer> temp2 = new ArrayList<Integer>();
			temp2.addAll(temp);
			for(Pair<String,Pair<String,String>> p : L1){
				String rel = p.first.replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
		    	String first = p.second.first.replaceAll("[^a-zA-Z0-9]", "");
		    	String second = p.second.second.replaceAll("[^a-zA-Z0-9]", "");
		    	if(first.startsWith("wh")){
		    		p.second.first = ans;
		    	}
		    	else {
		    		p.second.second = ans;
		    	}
		    	if(DependencySentence.containsKey(p)){
		    		temp2 = intersection2(temp2, DependencySentence.get(p));
		    	}
			}
			if(temp2.size() > 0){
				answer.add(ans);
			}
		}
		return answer;	
	}
	

	//removing the not usefull dependencies if they are present.
	public static Boolean checkThis(String first,String second,String rel){
		for(String s : notUseFull){
			if(first.compareTo(s)==0 || second.compareTo(s)==0)
				return true;
		}
		return false;
	}
	
	//to generate the query for single answer.
	public static String generateQuery (List<Pair<String,Pair<String,String>>> dependencies){
		tobeDone = new ArrayList<Pair<String,Pair<String,String>>>();
		Boolean flag = true;
		//SPARQL SELECT QUERY>
		String query = "SELECT * {";
		for(Pair<String,Pair<String,String>> L : dependencies){
			String rel = L.first.replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
	    	String first = L.second.first.replaceAll("[^a-zA-Z0-9]", "");
	    	String second = L.second.second.replaceAll("[^a-zA-Z0-9]", "");
	    	
	    	if(first.compareTo("")==0 || second.compareTo("")==0)
	    		continue;
	    	
	    	if(checkThis(first,second,rel)){
	    		tobeDone.add(L);
	    		continue;
	    	}
	    	
	    	Pair<String,Pair<String,String>> p = new Pair<String,Pair<String,String>>(rel,new Pair<String,String>(first,second));
	    	tobeCheked.add(p);
    		if(first.startsWith("wh")){
    			flag = false;
				query += ("\n" + "?x <http://"+rel+"> <http://" + second+"> .");
			}
			else if (second.startsWith("wh")){
				flag = false;
				query += ("\n <http://" + first +"> <http://"+rel+"> ?x .");
			}
			else{
				query += ("\n <http://" + first +"> <http://"+rel+"> <http://"+second+"> .");
			}
		}

		//if there is not node which can be questioned is present then include the not use full dependencies. To create a valid Query.
		if(flag == true){
			for(Pair<String,Pair<String,String>> L : dependencies){
			String rel = L.first.replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
		    	String first = L.second.first.replaceAll("[^a-zA-Z0-9]", "");
		    	String second = L.second.second.replaceAll("[^a-zA-Z0-9]", "");
		    	
		    	if(first.compareTo("")==0 || second.compareTo("")==0)
		    		continue;
		    	
			if(first.startsWith("wh")){
    				query += ("\n" + "?x <http://"+rel+"> <http://" + second+"> .");
			}
			else if (second.startsWith("wh")){
				query += ("\n <http://" + first +"> <http://"+rel+"> ?x .");
			}
		    }
		}
		query += "\n }";
		return query;
	}


	//To Generate the query for True False Type Answer. 
	public static String generateQuery2 (List<Pair<String,Pair<String,String>>> dependencies){
		String query = "ASK {";
		for(Pair<String,Pair<String,String>> L : dependencies){
			String rel = L.first.replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
	    	String first = L.second.first.replaceAll("[^a-zA-Z0-9]", "");
	    	String second = L.second.second.replaceAll("[^a-zA-Z0-9]", "");
	    	
	    	if(first.compareTo("")==0 || second.compareTo("")==0 || checkThis(first,second,rel))
	    		continue;
	    	
	    	Pair<String,Pair<String,String>> p = new Pair<String,Pair<String,String>>(rel,new Pair<String,String>(first,second));
	    	tobeCheked.add(p);
			query += ("\n <http://" + first +"> <http://"+rel+"> <http://"+second+"> .");
		}
		query += "\n }";
		return query;
	}

	//The main function.
	public static void main(String[] args) throws Exception{

		  DependencySentence = new HashMap<Pair<String,Pair<String,String>>, List<Integer>>();
		  tobeCheked = new ArrayList<Pair<String,Pair<String,String>>>();
		  //These are all the words whose dependencies are useless.
		  notUseFull.add("is");
		  notUseFull.add("am");
		  notUseFull.add("are");
		  notUseFull.add("will");
		  notUseFull.add("shall");
		  notUseFull.add("the");
		  notUseFull.add("a");
		  notUseFull.add("an");
		  notUseFull.add("do");
		  notUseFull.add("did");
		  notUseFull.add("does");
		  notUseFull.add("was");
		  notUseFull.add("were");
		  notUseFull.add("be");

		  //setting up the annotators for corenlp ..
		  props = new Properties();
		  props.put("annotators", "tokenize,ssplit,pos,lemma,ner,parse,dcoref");
		  props.put("dcoref.score",true);
		  pipeline = new StanfordCoreNLP(props);
		  String text = "";
		  String line = "";
		  if(args.length > 0){
		  	  //reading file.
			  System.out.println("Opening File : "+args[0]);
			  BufferedReader f = new BufferedReader(new FileReader(args[0]));
			  while((line=f.readLine())!=null){
				  text = text + " " + line;
			  }
			  //Running Pronoun Resolution
			  Map<Pair<Integer,Integer>,String> temp = PronounResolution.ResolvePronoun(text);
			  should = true;
			  //Extracting Dependencies.
			  List<Pair<String,Pair<String,String>>> l = DependecyRelationship.Dependencies(text, temp);

			  //Storing Dependencies
			  Database.addData(l);
			  f.close();

			  //Opening Question.
			  System.out.println("Opening File : "+args[1]);
			  f = new BufferedReader(new FileReader(args[1]));
			  line = f.readLine();
			  text = line;
			  
			  //Getting the first word
			  String s1 = text.split("\\s+")[0];
			  System.out.println(s1);

			  //Checking the first word if it wh-word or not.
			  if(s1.toLowerCase().startsWith("wh"))
			  {
			  		//Single word answer case
				  should = false;
				  List<Pair<String,Pair<String,String>>> l1 = DependecyRelationship.Dependencies(text, null);
				  String query = generateQuery(l1);
				  System.out.println("Finding Answers");
				  System.out.println(query); 

				  //Querying Jena for possible answers.
				  List<String> solution = Database.queryData(query);
				  System.out.println("Refining Answers");
				  BufferedWriter bw = new BufferedWriter(new FileWriter("shriraj2"));

				  //Applying Filters to answers.
				  for(String s: intersection(solution)){
					  bw.write(s+"\n");
				  }
				  bw.close();
			  }
			  else {
			  	//True False Answer case.
				  should = false;
				  List<Pair<String,Pair<String,String>>> l1 = DependecyRelationship.Dependencies(text, null);
				  String query = generateQuery2(l1);
				  System.out.println(query); 

				  //Querying Jena for possible answers.
				  Boolean solution = Database.queryData2(query);
				  BufferedWriter bw = new BufferedWriter(new FileWriter("shriraj2"));

				  System.out.println("Finding Answer");
				  
				  System.out.println("Finding Answer");
				  if(solution==false){
				  	  bw.write("false");
				  }
				  else{
				  	  //applying filters to answer.
					  System.out.println("Refining Answer");
					  Boolean ans = interseciton2();
					  bw.write(ans.toString());
				  }

				  bw.close();
			  }
		  }
		  else {
			  System.out.println("Please give program some argument");
		  }
	}
}
