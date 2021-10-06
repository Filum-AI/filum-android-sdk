package filum.android.sdk.exception;

public class FILUMInstanceExistsException extends FILUMRuntimeException {
    public FILUMInstanceExistsException() {
        super("Instance exists. Call getInstance() instead.");
    }
}
