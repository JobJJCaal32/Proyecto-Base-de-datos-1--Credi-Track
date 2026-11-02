package gt.umg.creditrack.seguridad;

public final class DescripcionEquipo {

    private DescripcionEquipo() {
    }

    public static String desde(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Desconocido";
        }
        return navegador(userAgent) + " en " + sistema(userAgent);
    }

    private static String navegador(String ua) {
        if (ua.contains("Edg/")) {
            return "Edge";
        }
        if (ua.contains("OPR/") || ua.contains("Opera")) {
            return "Opera";
        }
        if (ua.contains("Firefox/")) {
            return "Firefox";
        }
        if (ua.contains("Chrome/")) {
            return "Chrome";
        }
        if (ua.contains("Safari/")) {
            return "Safari";
        }
        return "Otro navegador";
    }

    private static String sistema(String ua) {
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("iPhone") || ua.contains("iPad")) {
            return "iOS";
        }
        if (ua.contains("Mac OS")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return "otro sistema";
    }
}
