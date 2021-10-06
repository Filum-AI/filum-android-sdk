package filum.android.sdk.exception;

public class FILUMInvalidPropsPrefixException extends FILUMRuntimeException {
    public FILUMInvalidPropsPrefixException() {
        super("Custom prop can not start with '_' character.");
    }
}
