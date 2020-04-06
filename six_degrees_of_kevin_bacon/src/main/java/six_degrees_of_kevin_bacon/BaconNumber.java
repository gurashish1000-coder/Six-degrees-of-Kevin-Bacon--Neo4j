package six_degrees_of_kevin_bacon;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
// Neo4J imports
import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;

public class BaconNumber implements HttpHandler, AutoCloseable
{
    private static Mem memory;
    Driver driver;
    
    public BaconNumber(Mem mem, Driver drive) {
        memory = mem;
        driver = drive;
    }
    
    @Override
	public void close() throws Exception {
		driver.close();
	}

    @SuppressWarnings("restriction")
	public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("GET")) 
                handleGet(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

@SuppressWarnings("restriction")
public void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);

        String Id = memory.getValue();
        long number = 0;
        
        if (deserialized.has("actorId"))
            Id = deserialized.getString("actorId");
        else {
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        
        try (Session session = driver.session())
        {	
        	try (Transaction tx = session.beginTransaction())
        	{
        		StatementResult actor_name = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a.Name", parameters("actorId", Id));
        		if(Id.equals("nm0000102")) { //KEVIN BACON RETURN
        			String response = "{\n\t" + 
        	        		"\"baconNumber\": \"0"
        	        		+ "\"\n}";
        			r.sendResponseHeaders(200, response.length()); //No path respond with 200 and undefined
        			OutputStream os = r.getResponseBody();
        	        os.write(response.getBytes());
        	        os.close();
        			return;
        		}
        		if(actor_name.hasNext()) { 
        			//actor_id exists
        			//retrieve movies since we know actorID is in the database
        			StatementResult shortest_path = tx.run("MATCH (a:actor),(b:actor)"
        											+"WHERE a.id = $actorId AND b.id = $kevinId "
        											+"MATCH p = shortestPath((a)-[*]-(b))"
        											+"RETURN p", parameters("actorId", Id, "kevinId", "nm0000102"));
        			tx.success();  // Mark this write as successful.
        			//Shortest path lists out every step can divide by 2 because each connection is a pair
        			number = shortest_path.list().toString().split(",").length / 2;
        			String response = "{\n\t\"baconNumber\": \"";
        			if(number == 0) {
        				response = response.concat("undefined\"\n}");
        				r.sendResponseHeaders(200, response.length());
            	        OutputStream os = r.getResponseBody();
            	        os.write(response.getBytes());
            	        os.close();
            	        return;
        			} else {
        				response = response.concat(number + "\"\n}");
        	        	r.sendResponseHeaders(200, response.length());
        	        	OutputStream os = r.getResponseBody();
        	        	os.write(response.getBytes());
        	        	os.close();
        	        	return;
        			}
        		} else {
        			r.sendResponseHeaders(400, -1); //No actor respond with 400 and undefined
        			return;
        		}
        	}
        }catch(Exception e) {
        	r.sendResponseHeaders(500, -1);
        	return;
        }
    }
}