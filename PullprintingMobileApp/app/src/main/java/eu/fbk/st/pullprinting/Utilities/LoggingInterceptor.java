package eu.fbk.st.pullprinting.Utilities;


import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public class LoggingInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long t1 = System.nanoTime();
        Log.d("OkHttp", String.format("--> Sending request %s on %s%n%s", request.url(), chain.connection(), request.headers()));

        Buffer requestBuffer = new Buffer();
        request.body().writeTo(requestBuffer);
        Log.d("OkHttp", requestBuffer.readUtf8());

        Response response = chain.proceed(request);

        long t2 = System.nanoTime();
        Log.d("OkHttp", String.format("<-- Received response for %s in %.1fms%n%s", response.request().url(), (t2 - t1) / 1e6d, response.headers()));

        MediaType contentType = response.body().contentType();
        String content = response.body().string();
        Log.d("OkHttp", content);

        ResponseBody wrappedBody = ResponseBody.create(contentType, content);
        return response.newBuilder().body(wrappedBody).build();
    }
}