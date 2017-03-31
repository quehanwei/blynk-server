package cc.blynk.server.api.http;

import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.UrlReWriterHandler;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.HttpAndWebSocketUnificatorHandler;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.dao.CSVGenerator;
import cc.blynk.utils.UrlMapper;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpAPIServer extends BaseServer {

    public static final String WEBSOCKET_PATH = "/websocket";
    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpAPIServer(Holder holder, boolean isUnpacked) {
        super(holder.props.getProperty("listen.address"), holder.props.getIntProperty("http.port"), holder.transportTypeHolder);

        String rootPath = holder.props.getProperty("admin.rootPath", "/dashboard");

        HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler = new HttpAndWebSocketUnificatorHandler(holder, port, rootPath);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("HttpServerCodec", new HttpServerCodec());
                pipeline.addLast("HttpObjectAggregator", new HttpObjectAggregator(65536, true));
                pipeline.addLast(new ChunkedWriteHandler());
                pipeline.addLast(new UrlReWriterHandler(new UrlMapper("/favicon.ico", "/static/favicon.ico"),
                        new UrlMapper(rootPath, "/static/index.html")));
                pipeline.addLast(new StaticFileHandler(isUnpacked, new StaticFile("/static"),
                                new StaticFileEdsWith(CSVGenerator.CSV_DIR, ".csv.gz"))
                );
                pipeline.addLast("HttpWebSocketUnificator", httpAndWebSocketUnificatorHandler);
            }
        };
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTP API and WebSockets";
    }

    @Override
    public void close() {
        System.out.println("Shutting down HTTP API and WebSockets server...");
        super.close();
    }

}
