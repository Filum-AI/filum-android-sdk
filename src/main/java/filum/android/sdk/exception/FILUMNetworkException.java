package filum.android.sdk.exception;

public class FILUMNetworkException extends FILUMRuntimeException {
    public FILUMNetworkException(int code) {
        super("Request failed: " + code);
    }

    public FILUMNetworkException(int code, String responseBody) {
        super("Request failed: " + code + " - " + responseBody);
    }
}
