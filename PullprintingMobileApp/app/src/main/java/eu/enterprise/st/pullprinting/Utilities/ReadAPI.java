package eu.enterprise.st.pullprinting.Utilities;

import java.io.IOException;

import okhttp3.Request;
import okio.Buffer;

public class ReadAPI {

    public static String bodyToString(final Request request){

        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "did not work";
        }
    }

}