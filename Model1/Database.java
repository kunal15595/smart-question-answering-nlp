import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.GraphStore;
import org.apache.jena.update.GraphStoreFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

import com.fasterxml.jackson.core.PrettyPrinter;

import edu.stanford.nlp.util.Pair;


public class Database {
	//adding Data to Database.
	@SuppressWarnings("deprecation")
	public static void addData(List<Pair<String,Pair<String,String>>> l) throws IOException{
		//removing the old database
		FileUtils.deleteDirectory(new File("temp/dataset")); 

		//creating new database
		Dataset db = TDBFactory.createDataset("temp/dataset");

		db.begin(ReadWrite.WRITE);
		GraphStore graphStore = GraphStoreFactory.create(db) ;

		//writing all the dependencies.
	    for(Pair<String,Pair<String,String>> p : l){
	    	String rel = p.first.replaceAll("[^a-zA-Z0-9]+[A-Za-z]*", "");
	    	String first = p.second.first.replaceAll("[^a-zA-Z0-9]", "");
	    	String second = p.second.second.replaceAll("[^a-zA-Z0-9]", "");	
	    	if(first.compareTo("")==0 || second.compareTo("")==0)
	    		continue;
			String sparqlUpdateString = "INSERT DATA { <http://"+first+"> <http://"+rel+"> <http://"+second+"> }";
	        UpdateRequest request = UpdateFactory.create(sparqlUpdateString) ;
	        UpdateProcessor proc = UpdateExecutionFactory.create(request, graphStore) ;
	        proc.execute() ;
	        sparqlUpdateString = "INSERT DATA { <http://"+second+"> <http://"+rel+"> <http://"+first+"> }";
	        request = UpdateFactory.create(sparqlUpdateString) ;
	        proc = UpdateExecutionFactory.create(request, graphStore) ;
	        proc.execute() ;
	    }
	    //commiting the database.
        db.commit() ;

        //closing the database.
	    db.close();
	}

	//querying the Data for single answer query. Select query.
	public static List<String> queryData(String query){
		Dataset db = TDBFactory.createDataset("temp/dataset");
		db.begin(ReadWrite.READ);
	    Model model = db.getDefaultModel();
	    Query q = QueryFactory.create(query);
	    QueryExecution qexec = QueryExecutionFactory.create(query, model);
	    ResultSet results = qexec.execSelect();
	    List<String> answer = new ArrayList<String>();
	    while(results.hasNext()){
	    	QuerySolution t = results.nextSolution();
	    	RDFNode x  = t.get("x");
	    	String s = x.toString();
		System.out.println(s);
	    	answer.add(s.substring(7));
	    }
	    qexec.close();
	    db.close();
	    return answer;
	}
	
	//querying the Data for true false answer query. ASK Query.
	public static Boolean queryData2(String query){
		Dataset db = TDBFactory.createDataset("temp/dataset");
		db.begin(ReadWrite.READ);
	    Model model = db.getDefaultModel();
	    Query q = QueryFactory.create(query);
	    QueryExecution qexec = QueryExecutionFactory.create(query, model);
	    List<String> answer = new ArrayList<String>();
	    Boolean t = (qexec.execAsk());
	    qexec.close();
	    db.close();
	    return t;
	}
	
	public static void main(String[] args){
		
	}
}
