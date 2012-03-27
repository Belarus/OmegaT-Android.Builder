import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import sun.misc.BASE64Encoder;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

public class TranslateSignatures {

    private static class SignatureOutputStream extends FilterOutputStream {

        public void write(int b) throws IOException {
            try {
                mSignature.update((byte) b);
            } catch (SignatureException e) {
                throw new IOException((new StringBuilder()).append("SignatureException: ").append(e)
                        .toString());
            }
            super.write(b);
        }

        public void write(byte b[], int off, int len) throws IOException {
            try {
                mSignature.update(b, off, len);
            } catch (SignatureException e) {
                throw new IOException((new StringBuilder()).append("SignatureException: ").append(e)
                        .toString());
            }
            super.write(b, off, len);
        }

        private Signature mSignature;

        public SignatureOutputStream(OutputStream out, Signature sig) {
            super(out);
            mSignature = sig;
        }
    }

    X509Certificate publicKey;
    PrivateKey privateKey;
    Manifest outputManifest;

    public TranslateSignatures() throws Exception {

        publicKey = readPublicKey(Translate.class.getResourceAsStream("testkey.x509.pem"));
        privateKey = readPrivateKey(Translate.class.getResourceAsStream("testkey.pk8"));

        outputManifest = new Manifest();
        outputManifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        outputManifest.getMainAttributes().putValue("Created-By", "1.0 (Android SignApk)");
    }

    public void putDigestToManifest(ZipEntry en, byte[] data) throws Exception {
        Attributes attrs = new Attributes();
        attrs.putValue("SHA1-Digest", sha1base64(data));
        outputManifest.getEntries().put(en.getName(), attrs);
    }

    public void writeTo(ZipOutputStream zipOut) throws Exception {
        zipOut.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        outputManifest.write(zipOut);

        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        zipOut.putNextEntry(new JarEntry("META-INF/CERT.SF"));
        writeSignatureFile(outputManifest, new SignatureOutputStream(zipOut, signature));
        zipOut.putNextEntry(new JarEntry("META-INF/CERT.RSA"));
        writeSignatureBlock(signature, publicKey, zipOut);
    }

    private static X509Certificate readPublicKey(InputStream input) throws IOException,
            GeneralSecurityException {
        X509Certificate x509certificate;
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        x509certificate = (X509Certificate) cf.generateCertificate(input);
        input.close();
        return x509certificate;
    }

    private static PrivateKey readPrivateKey(InputStream in) throws IOException, GeneralSecurityException {
        DataInputStream input = new DataInputStream(in);
        KeySpec spec;
        byte bytes[] = new byte[8192];
        input.read(bytes);
        spec = new PKCS8EncodedKeySpec(bytes);
        PrivateKey privatekey = KeyFactory.getInstance("RSA").generatePrivate(spec);
        input.close();
        return privatekey;
    }

    private static void writeSignatureFile(Manifest manifest, OutputStream out) throws IOException,
            GeneralSecurityException {
        Manifest sf = new Manifest();
        Attributes main = sf.getMainAttributes();
        main.putValue("Signature-Version", "1.0");
        main.putValue("Created-By", "1.0 (Android SignApk)");
        BASE64Encoder base64 = new BASE64Encoder();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        PrintStream print = new PrintStream(new DigestOutputStream(new ByteArrayOutputStream(), md), true,
                "UTF-8");
        manifest.write(print);
        print.flush();
        main.putValue("SHA1-Digest-Manifest", base64.encode(md.digest()));
        Map entries = manifest.getEntries();
        java.util.Map.Entry<String, Attributes> entry;
        Attributes sfAttr;
        for (Iterator i$ = entries.entrySet().iterator(); i$.hasNext(); sf.getEntries().put(entry.getKey(),
                sfAttr)) {
            entry = (java.util.Map.Entry) i$.next();
            print.print((new StringBuilder()).append("Name: ").append((String) entry.getKey()).append("\r\n")
                    .toString());
            java.util.Map.Entry att;
            for (Iterator j$ = ((Attributes) entry.getValue()).entrySet().iterator(); j$.hasNext(); print
                    .print((new StringBuilder()).append(att.getKey()).append(": ").append(att.getValue())
                            .append("\r\n").toString()))
                att = (java.util.Map.Entry) j$.next();

            print.print("\r\n");
            print.flush();
            sfAttr = new Attributes();
            sfAttr.putValue("SHA1-Digest", base64.encode(md.digest()));
        }

        sf.write(out);
    }

    private static void writeSignatureBlock(Signature signature, X509Certificate publicKey, OutputStream out)
            throws IOException, GeneralSecurityException {
        SignerInfo signerInfo = new SignerInfo(new X500Name(publicKey.getIssuerX500Principal().getName()),
                publicKey.getSerialNumber(), AlgorithmId.get("SHA1"), AlgorithmId.get("RSA"),
                signature.sign());
        PKCS7 pkcs7 = new PKCS7(new AlgorithmId[] { AlgorithmId.get("SHA1") }, new ContentInfo(
                ContentInfo.DATA_OID, null), new X509Certificate[] { publicKey },
                new SignerInfo[] { signerInfo });
        pkcs7.encodeSignedData(out);
    }

    static String sha1base64(byte[] data) throws Exception {
        BASE64Encoder base64 = new BASE64Encoder();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(data, 0, data.length);
        return base64.encode(md.digest());
    }

}
