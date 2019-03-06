package de.pauhull.nickapi.gson;

/**
 * Created by Paul
 * on 05.03.2019
 *
 * @author pauhull
 */
public class NickData {

    public Response response;

    public System system;

    public static class Response {

        public String playername;

        public Skin skin;

        public static class Skin {

            public String url;

            public String value;

            public String signature;

        }

    }

    public static class System {

        public int status;

        public String message;

    }

}
