package six_degrees_of_kevin_bacon;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.json.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.*;
import static org.neo4j.driver.v1.Values.parameters;


public class Movie implements HttpHandler {
	private static Mem memory;
	Driver driver;

	public Movie(Mem mem, Driver drvr) {
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
	private void handleGet(HttpExchange r) throws IOException, JSONException {
		// TODO Auto-generated method stub
		String body = Utils.convert(r.getRequestBody());
		JSONObject deserialized;

		try {
			deserialized = new JSONObject(body);
		} catch (Exception e) {
			//If Error parsing the JSON Message
			r.sendResponseHeaders(400, -1);
			return;
		}

		String movieId = memory.getValue();
		StatementResult movie_name;
		StatementResult movie_actors;

		if (deserialized.has("movieId"))
			movieId = deserialized.getString("movieId");
		else {
			//no movieId in input
			r.sendResponseHeaders(400, -1);
			return;
		}

		try (Session session = driver.session())
		{	
			System.out.println(movieId);
			try (Transaction tx = session.beginTransaction())
			{	
				movie_name = tx.run("MATCH (a:movie) WHERE a.id = $movieId RETURN a.Name", parameters("movieId", movieId));
				if(movie_name.hasNext()) { 
					//movieId exists
					//retrieve movies since we know actorID is in the database
					movie_actors = tx.run("MATCH (:movie { id: {x} })--(actor) RETURN actor.id", parameters("x", movieId));
					tx.success();  
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


		String actors_list = "\n\t\t";
		//put list of movies into a long string
		List<Record> results = movie_actors.list(); // store .list() it makes it empty after using .list() once
		if (results.isEmpty()) 
			actors_list = "";
		else {
			for (int i = 0; i < results.size(); i++) {
				actors_list = actors_list + results.get( i ).get("actor.id");
				if (i != results.size() -1)
					actors_list += ",\n\t\t";
			}
			actors_list += "\n\t";
		}

		String response = "{\n\t" + 
				"\"movieId\": " + "\"" + movieId + "\",\n\t" +
				"\"name\": " + "\"" + movie_name.single().get( 0 ).asString() + "\",\n\t" + 
				"\"actors\": " + 
				"[" + actors_list + "]"
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

		String name = memory.getMovieName();
		String movieId = memory.getMovieId();

		if (deserialized.has("name") && deserialized.has("movieId")) {
			name = deserialized.getString("name");
			movieId = deserialized.getString("movieId");
		} else {
			r.sendResponseHeaders(400, -1);
			return;
		}

		try (Session session = driver.session())
		{
			
			try (Transaction tx = session.beginTransaction())
			{
				// Making sure that there are not any duplicates.
				StatementResult result = tx.run("MATCH (a:movie) WHERE a.id = $movieId RETURN a", parameters("movieId", movieId));
				if(!result.hasNext()) {
        			// Wrapping Cypher in an explicit transaction provides atomicity
                    // and makes handling errors much easier
					tx.run("CREATE (a:movie {Name: {x1}, id: {x2}})", parameters("x1", name, "x2", movieId));
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