package six_degrees_of_kevin_bacon;

import java.io.IOException;
import java.io.OutputStream;
import org.json.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;


public class Relationship implements HttpHandler {
    private static Mem memory;
    Driver driver;

    public Relationship(Mem mem, Driver drvr) {
        memory = mem;
        driver = drvr;
    }

    @SuppressWarnings("restriction")
	public void handle(HttpExchange r) {
        try {
        	if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            } else if (r.getRequestMethod().equals("PUT")) {
            	handlePut(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("restriction")
	private void handleGet(HttpExchange r) throws IOException, JSONException{
		// TODO Auto-generated method stub
    	String body = Utils.convert(r.getRequestBody());
    	boolean relationship = false;
     	JSONObject deserialized;
     	try {
     		deserialized = new JSONObject(body);
     	} catch (Exception e) {
     		//Error parsing the JSON Message
     		r.sendResponseHeaders(400, -1);
     		return;
     	}
     	
     	String actorId = memory.getValue();
        String movieId = memory.getValue();
        
        if (deserialized.has("actorId") && deserialized.has("movieId")) {
        	actorId = deserialized.getString("actorId");
        	movieId = deserialized.getString("movieId");
        } else {
        	//missing one of actorId or movieId
        	r.sendResponseHeaders(400, -1);
        	return;
        }
     	//create node 
        // Sessions are lightweight and disposable connection wrappers.
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{
        		StatementResult actor_name = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a.Name", parameters("actorId", actorId));
        		if(!actor_name.hasNext()) { 
        			//actor_id DNE
        			r.sendResponseHeaders(404, -1); //SEND 404 NOT FOUND IF NAME ISNT FOUND I.E NO ACTORID IN DB
        			return;
        		}
        		StatementResult movie_name = tx.run("MATCH (a:movie) WHERE a.id = $movieId RETURN a", parameters("movieId", movieId));
        		if(!movie_name.hasNext()) {
        			//movie_id DNE
        			r.sendResponseHeaders(404, -1); //SEND 404 NOT FOUND IF NAME ISNT FOUND I.E NO MOVIEID IN DB
        			return;
        		}
        		StatementResult result = tx.run("MATCH (movie { id: $movieId })<-[r:ACTED_IN]-(actor { id: $actorId})" + 
        										"RETURN movie.Name", parameters("actorId", actorId, "movieId", movieId));
        		tx.success();  // Mark this write as successful.
        		if(result.hasNext())
        			relationship = true;
        		
        		String response = "{\n\t" + 
        		     		"\"actorId\": " + "\"" + actorId + "\"\n\t" +
        		     		"\"movieId\": " + "\"" + movieId + "\"\n\t" + 
        		     		"\"hasRelationship\": " + relationship + "\n}";
        		//relationship does not exist bool stays false
        		r.sendResponseHeaders(200, response.length());
        		OutputStream os = r.getResponseBody();
        		os.write(response.getBytes());
        		os.close();
        		return;
        	}
        } catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	return;
        }  
		
	}

	@SuppressWarnings("restriction")
	public void handlePut(HttpExchange r) throws IOException, JSONException{
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);
        
        String actorId;
        String movieId;

        if (deserialized.has("actorId") && deserialized.has("movieId")) {
        	actorId = deserialized.getString("actorId");
            movieId = deserialized.getString("movieId");
        } else {
            r.sendResponseHeaders(400, -1);
            return;
        }
        
        
        try (Session session = driver.session())
        {
            try (Transaction tx = session.beginTransaction())
            {
            	StatementResult actor_name = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a.Name", parameters("actorId", actorId));
        		if(!actor_name.hasNext()) { //actor_id DNE
        			r.sendResponseHeaders(404, -1); //SEND 404 NOT FOUND IF NAME ISNT FOUND I.E NO ACTORID IN DB
        			return;
        		}
        		StatementResult movie_name = tx.run("MATCH (a:movie) WHERE a.id = $movieId RETURN a", parameters("movieId", movieId));
        		if(!movie_name.hasNext()) { //movie_id DNE
        			//movie_id DNE
        			r.sendResponseHeaders(404, -1); //SEND 404 NOT FOUND IF NAME ISNT FOUND I.E NO MOVIEID IN DB
        			return;
        		}
        		//CHECK IF RELATIONSHIP EXISTS ALREADY
        		StatementResult relationshipCheck = tx.run("MATCH (:actor { id: {x} })-[r:ACTED_IN]->(:movie { id: {y}}) RETURN type(r)", parameters("x", actorId, "y", movieId));
        		if(relationshipCheck.hasNext()) {	
        			//relationship already exists
        			r.sendResponseHeaders(400, -1);
        			return;
        		}
                tx.run("MATCH (a:actor),(m:movie) WHERE a.id = {x1} AND m.id = {x2} CREATE (a)-[r:ACTED_IN]->(m)", parameters("x1", actorId, "x2", movieId));
                tx.success();
                r.sendResponseHeaders(200 , -1);
            }
            
        }catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	System.out.println(e.toString());
        	System.out.println("SOMETHING WENT WRONG");
        	return;
        }
    }
}