package six_degrees_of_kevin_bacon;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.json.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;


public class Actor implements HttpHandler {
    private static Mem memory;
    Driver driver;

    public Actor(Mem mem, Driver drvr) {
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

    private void handleGet(HttpExchange r) throws IOException, JSONException {
		// TODO Auto-generated method stub
    	String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized;
        
        try {
	 		deserialized = new JSONObject(body);
	 	} catch (Exception e) {
	 		//Error parsing the JSON Message
	 		r.sendResponseHeaders(400, -1);
	 		return;
	 	}

        String Id = memory.getValue();
        StatementResult actor_name;
        StatementResult actor_movies;
        
        if (deserialized.has("actorId"))
            Id = deserialized.getString("actorId");
        else {
       	//no actorId in input
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{	
        		actor_name = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a.Name", parameters("actorId", Id));
        		if(actor_name.hasNext()) { 
        			//movieId exists
        			//retrieve movies since we know actorID is in the database
        			actor_movies = tx.run("MATCH (:actor { id: {x} })--(movie) RETURN movie.id", parameters("x", Id));
        			tx.success();  // Mark this write as successful.
        		} else {
        			System.out.println("-----------------------------------------------------------");
        			r.sendResponseHeaders(404, -1); //SEND 404 NOT FOUND IF NAME ISNT FOUND I.E NO movieId IN DB
        			return;
        		}
        	}
        }catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	System.out.println(e.toString());
        	return;
        }
        
        String movies_list = "\n\t\t";
        //put list of movies into a long string
        List<Record> results = actor_movies.list(); // store .list() it makes it empty after using .list() once
        if (results.isEmpty()) 
       	 movies_list = "";
        else {
        	for (int i = 0; i < results.size(); i++) {
        		movies_list = movies_list + results.get( i ).get("movie.id");
        		if (i != results.size() -1)
        			movies_list += ",\n\t\t";
        	}
        	movies_list += "\n\t";
        }
        
        String response = "{\n\t" + 
        		"\"actorId\": " + "\"" + Id + "\",\n\t" +
        		"\"name\": " + "\"" + actor_name.single().get( 0 ).asString() + "\",\n\t" + 
        		"\"movies\": " + 
        			"[" + movies_list + "]"
        		+ "\n}";
        		
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
    	
		
	}
    @SuppressWarnings("restriction")
	public void handlePut(HttpExchange r) throws IOException, JSONException{
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized;
        
        try {
			deserialized = new JSONObject(body);
		} catch (Exception e) {
			r.sendResponseHeaders(400, -1);
			return;
		}

        String name = memory.getActorName();
        String actorId = memory.getActorId();

        if (deserialized.has("name") && deserialized.has("actorId")) {
        	name = deserialized.getString("name");
            actorId = deserialized.getString("actorId");
        } else {
            r.sendResponseHeaders(400, -1);
            return;
        }      
        
        try (Session session = driver.session())
        {
            try (Transaction tx = session.beginTransaction())
            {
            	
            	// Making sure that there are not any duplicates.
				StatementResult result = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a", parameters("actorId", actorId));
				if(!result.hasNext()) {
        			// Wrapping Cypher in an explicit transaction provides atomicity
                    // and makes handling errors much easier
					tx.run("CREATE (a:actor {Name: {x1}, id: {x2}})", parameters("x1", name, "x2", actorId));
                    tx.success();  
                    r.sendResponseHeaders(200, -1);
                    return;
        		} else {
        			//movie does exist
        			r.sendResponseHeaders(400, -1);
        			return;
        		}
            } 
        }catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	System.out.println(e.toString());
        	System.out.println("SOMETHING WENT WRONG");
        	return;
        }
    }
}