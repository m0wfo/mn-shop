(ns shop.keystore
  (:import [java.security KeyStore]
           [javax.net.ssl
            SSLContext
            KeyManagerFactory
            TrustManagerFactory]
           [java.io FileInputStream]
           [java.lang System]))

(defn setup [path]
  (let [keystore (KeyStore/getInstance (KeyStore/getDefaultType))
        fis (FileInputStream. path)]
    (println "Enter passphrase for keystore:")
    (let [password (char-array "marsbars")]
      (try
        (. keystore load fis password)
        (catch Exception e
          (do
            (println "Invalid passphrase.")
            (. fis close)
            (System/exit 1))))
      (. fis close)
      (let [kmf (KeyManagerFactory/getInstance "SunX509")
            tmf (TrustManagerFactory/getInstance "SunX509")]
        (. kmf init keystore password)
        (. tmf init keystore)
        (println "Keystore successfully loaded.")
        (let [context (SSLContext/getInstance "TLSv1")]
          (. context init
             (. kmf getKeyManagers)
             (. tmf getTrustManagers) nil)
          context)))))

