(ns shop.handler
  (:import [org.jboss.netty.channel SimpleChannelUpstreamHandler]
           [org.jboss.netty.handler.codec.http
            HttpMethod
            HttpHeaders
            HttpHeaders$Names
            HttpResponseStatus
            HttpVersion
            HttpRequest
            DefaultHttpResponse]
           [org.jboss.netty.handler.codec.http.websocketx
            WebSocketFrame
            CloseWebSocketFrame
            PingWebSocketFrame
            PongWebSocketFrame
            TextWebSocketFrame
            WebSocketServerHandshaker
            WebSocketServerHandshakerFactory]))


(defn handler []
  (proxy [SimpleChannelUpstreamHandler] []
    (messageReceived [ctx e]
      (println "msg")
      (let [msg (. e getMessage)
            handshaker (atom nil)
            channel (. ctx getChannel)]
        (if (instance? HttpRequest msg)
          (let [host (HttpHeaders$Names/HOST)
                location (str "wss://" (. msg getHeader host) "/")
                wsfactory (WebSocketServerHandshakerFactory. location nil false)]
            (reset! handshaker (. wsfactory newHandshaker msg))
        (if (nil? @handshaker)
          (. wsfactory sendUnsupportedWebSocketVersionResponse channel)
          (do
            (let [f (. @handshaker handshake channel msg)]
              (. f addListener WebSocketServerHandshaker/HANDSHAKE_LISTENER))))))
        (if (instance? WebSocketFrame msg)
          (do
            (println "ws")
            (if (instance? CloseWebSocketFrame msg) (. @handshaker close channel msg))
            (if (instance? PingWebSocketFrame msg) (. channel write (PongWebSocketFrame. (. msg getBinaryData))))
            (if (instance? TextWebSocketFrame msg) (do
                                                (println "txt")
                                                (. channel write (TextWebSocketFrame. (. msg getText)))))))))
    (exceptionCaught [ctx e]
      (. (. e getChannel) close))))
