package six_degrees_of_kevin_bacon;

public class Mem {
	private static String actorName = "";
    private static String movieName = "";
    private static String actorId = "";
    private static String movieId = "";
    private static String value;

    public String getActorName() {
        return actorName;
    }
    
    public String getMovieName() {
        return movieName;
    }
    
    public String getActorId() {
        return actorId;
    }
    
    public String getMovieId() {
        return movieId;
    }

    public void setActor(String newActorName, String newActorId) {
        actorName = newActorName;
        actorId = newActorId;
    }
    
    public void setMovie(String newMovieName, String newMovieId) {
        movieName = newMovieName;
        movieId = newMovieId;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String newVal) {
        value = newVal;
    }
    
    public Mem() {}

}
