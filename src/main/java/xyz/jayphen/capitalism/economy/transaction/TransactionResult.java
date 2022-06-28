package xyz.jayphen.capitalism.economy.transaction;

public class TransactionResult {
		TransactionResultType type        = null;
		String                hash        = null;
		String                errorReason = "no error reason provided";
		
		public TransactionResult(TransactionResultType type, String hash) {
				this.type = type;
				this.hash = hash;
		}
		
		public TransactionResult(TransactionResultType type, String hash, String error) {
				this.type        = type;
				this.hash        = hash;
				this.errorReason = error;
		}
		
		public static String getLangFromResult(TransactionResultType res) {
				switch (res) {
						case SUCCESS:
								return "success";
						case FAIL:
								return "insufficient funds";
						default:
								return "error";
				}
		}
		
		public String getErrorReason() {
				return errorReason;
		}
		
		public TransactionResultType getType() {
				return type;
		}
		
		public String getHash() {
				return hash;
		}
		
		public enum TransactionResultType {
				SUCCESS, FAIL, ERROR
		}
}
