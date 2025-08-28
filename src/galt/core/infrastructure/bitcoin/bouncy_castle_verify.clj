(ns galt.core.infrastructure.bitcoin.bouncy-castle-verify
  (:require
    [galt.core.infrastructure.bitcoin.encoding :as encoding])
  (:import
    [org.bouncycastle.asn1 ASN1InputStream]
    [org.bouncycastle.asn1.sec SECNamedCurves]
    [org.bouncycastle.crypto.params ECDomainParameters]
    [org.bouncycastle.crypto.params ECPublicKeyParameters]
    [org.bouncycastle.crypto.signers ECDSASigner]))

; Docs: https://downloads.bouncycastle.org/java/docs/bcprov-jdk18on-javadoc/
(defn verify-signature
  "Verifies an LNURL-auth ECDSA signature.
   - k1-hex: Hex string of the 32-byte challenge.
   - pub-hex: Hex string of the compressed 33-byte public key.
   - sig-hex: Hex string of the DER-encoded signature.
   Returns true if valid, false otherwise."
  [k1-hex pub-hex sig-hex]
  (try
    (let [x9 (. SECNamedCurves getByName "secp256k1")
          ec (ECDomainParameters. (.getCurve x9) (.getG x9) (.getN x9) (.getH x9))
          pub-bytes (encoding/hex-decode pub-hex)
          point (.decodePoint (.getCurve ec) pub-bytes)
          pub-key-params (ECPublicKeyParameters. point ec)
          sig-bytes (encoding/hex-decode sig-hex)]
      (with-open [asn1 (ASN1InputStream. sig-bytes)]
        (let [seq (.readObject asn1)
              r (.getPositiveValue (.getObjectAt seq 0))
              s (.getPositiveValue (.getObjectAt seq 1))
              signer (ECDSASigner.)]
          (.init signer false pub-key-params)
          (.verifySignature signer (encoding/hex-decode k1-hex) r s))))
    (catch Exception _ false)))
