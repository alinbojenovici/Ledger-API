package backend.ledger_api.exception;

public class AccountConflictException extends RuntimeException { private final Object[] arguments; public AccountConflictException(String key,Object... arguments){super(key);this.arguments=arguments;} public Object[] getArguments(){return arguments;} }
