package xyz.jayphen.capitalism.economy.transaction;

public class TransactionResult {
    public enum TransactionResultType {
        SUCCESS,
        FAIL,
        ERROR
    }
    public static String getLangFromResult(TransactionResultType res) {
        switch(res) {
            case SUCCESS: return "success";
            case FAIL: return "insufficient funds";
            default: return "error";
        }
    }
    TransactionResultType type = null;
    String hash = null;

    public String getErrorReason() {
        return errorReason;
    }

    String errorReason = "no error reason provided";
    public TransactionResult(TransactionResultType type, String hash) {
        this.type = type;
        this.hash = hash;
    }
    public TransactionResult(TransactionResultType type, String hash, String error) {
        this.type = type;
        this.hash = hash;
        this.errorReason = error;
    }
    public TransactionResultType getType() {
        return type;
    }

    public String getHash() {
        return hash;
    }
}
