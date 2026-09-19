package net.openconnect_vpn.android;

public class OcProfileParser {

    private enum Section {
        NONE,
        CERTIFICATE,
        PRIVATE_KEY,
        CA_CERTIFICATE
    }

    public static OcProfile parse(String text) throws Exception {
        if (text == null)
            throw new Exception("Profile is empty");

        String[] lines = text.split("\\r?\\n");

        if (lines.length == 0 ||
                !lines[0].trim().equals("OCONNECT_PROFILE_V1")) {
            throw new Exception("Unsupported profile format");
        }

        OcProfile profile = new OcProfile();

        profile.port = 443;

        StringBuilder certificate = new StringBuilder();
        StringBuilder privateKey = new StringBuilder();
        StringBuilder caCertificate = new StringBuilder();

        Section section = Section.NONE;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.equals("[CERTIFICATE]")) {
                section = Section.CERTIFICATE;
                continue;
            }

            if (trimmed.equals("[PRIVATE_KEY]")) {
                section = Section.PRIVATE_KEY;
                continue;
            }

            if (trimmed.equals("[CA_CERTIFICATE]")) {
                section = Section.CA_CERTIFICATE;
                continue;
            }

            if (section == Section.CERTIFICATE) {
                certificate.append(line).append('\n');
                continue;
            }

            if (section == Section.PRIVATE_KEY) {
                privateKey.append(line).append('\n');
                continue;
            }

            if (section == Section.CA_CERTIFICATE) {
                caCertificate.append(line).append('\n');
                continue;
            }

            int pos = line.indexOf('=');

            if (pos < 0)
                continue;

            String key = line.substring(0, pos).trim();
            String value = line.substring(pos + 1).trim();

            if (key.equals("name")) {
                profile.name = value;
            } else if (key.equals("address")) {
                profile.address = value;
            } else if (key.equals("port")) {
                try {
                    profile.port = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid port");
                }
            } else if (key.equals("code_word")) {
                profile.codeWord = value;
            }
        }

        profile.certificate = certificate.toString().trim();
        profile.privateKey = privateKey.toString().trim();
        profile.caCertificate = caCertificate.toString().trim();

        validate(profile);

        return profile;
    }

    private static void validate(OcProfile profile) throws Exception {
        if (profile.address == null || profile.address.isEmpty())
            throw new Exception("Missing address");

        if (profile.port < 1 || profile.port > 65535)
            throw new Exception("Invalid port");

        if (profile.certificate.isEmpty())
            throw new Exception("Missing certificate");

        if (profile.privateKey.isEmpty())
            throw new Exception("Missing private key");

        if (profile.caCertificate.isEmpty())
            throw new Exception("Missing CA certificate");

        if (!profile.certificate.contains("-----BEGIN CERTIFICATE-----") ||
                !profile.certificate.contains("-----END CERTIFICATE-----"))
            throw new Exception("Invalid certificate");

        if (!profile.caCertificate.contains("-----BEGIN CERTIFICATE-----") ||
                !profile.caCertificate.contains("-----END CERTIFICATE-----"))
            throw new Exception("Invalid CA certificate");

        if (!profile.privateKey.contains("-----BEGIN PRIVATE KEY-----") &&
                !profile.privateKey.contains("-----BEGIN RSA PRIVATE KEY-----") &&
                !profile.privateKey.contains("-----BEGIN EC PRIVATE KEY-----"))
            throw new Exception("Invalid private key");
    }
}