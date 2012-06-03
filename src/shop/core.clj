(ns shop.core
  (:require [shop.keystore :as ks]
            [shop.handler :as handler]
            [shop.fs :as fs])
  (:import [java.net InetSocketAddress]
          [java.util.concurrent Executors]
          [org.jboss.netty.bootstrap ServerBootstrap]
          [org.jboss.netty.buffer ChannelBuffers]
          [org.jboss.netty.channel Channels ChannelPipelineFactory SimpleChannelUpstreamHandler]
          [org.jboss.netty.channel.socket.nio NioServerSocketChannelFactory]
          [org.jboss.netty.handler.stream ChunkedWriteHandler]
          [org.jboss.netty.handler.codec.http
           HttpChunkAggregator
           HttpRequestDecoder
           HttpResponseEncoder]
          [org.jboss.netty.handler.ssl SslHandler]))

(defn pipeline [ctx]
  (proxy [ChannelPipelineFactory] []
    (getPipeline []
      (let [pipe (Channels/pipeline)
            engine (. ctx createSSLEngine)]
        (. engine setUseClientMode false)
        (. pipe addLast "ssl" (SslHandler. engine))
        (. pipe addLast "decoder" (HttpRequestDecoder.))
        (. pipe addLast "aggregator" (HttpChunkAggregator. 65536))
        (. pipe addLast "encoder" (HttpResponseEncoder.))
        (. pipe addLast "streamer" (ChunkedWriteHandler.))
        (. pipe addLast "handler" (handler/handler))
        pipe))))

(defn boot []
  (let [address (InetSocketAddress. 8080)
        factory (NioServerSocketChannelFactory.)
        bootstrap (ServerBootstrap. factory)
        pl (pipeline (ks/setup "/home/chris/.keystore"))]
    (. bootstrap setPipelineFactory pl)
    (. bootstrap bind address)))
