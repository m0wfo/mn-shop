(ns shop.fs
  (:import
   [org.jboss.netty.buffer ChannelBuffers]
   [org.jboss.netty.channel
    ChannelFutureListener
    SimpleChannelUpstreamHandler]
   [org.jboss.netty.handler.stream
    ChunkedFile
    ChunkedWriteHandler]
   [org.jboss.netty.handler.codec.http
    HttpMethod
    HttpHeaders
    HttpHeaders$Names
    HttpResponseStatus
    HttpVersion
    HttpRequest
    DefaultHttpResponse]
   [java.net URI]
   [java.nio.file Files Paths]))

(def page "file:///home/chris/Documents/business/shop/resources/index.html")

(defn serve [ctx e]
  (let [msg (. e getMessage)
        channel (. e getChannel)
        path (Paths/get (URI. page))
        bytes (Files/readAllBytes path)
        data (ChannelBuffers/copiedBuffer bytes)
        response (DefaultHttpResponse. HttpVersion/HTTP_1_1 HttpResponseStatus/OK)]
    (. response setContent data)
    (let [op (. channel write response)]
      (. op addListener (proxy [ChannelFutureListener] []
                         (operationComplete [f]
                           (. (. f getChannel) close)))))))
