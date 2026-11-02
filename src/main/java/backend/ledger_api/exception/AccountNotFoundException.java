package backend.ledger_api.exception;

public class AccountNotFoundException extends RuntimeException { private final Object[] arguments; public AccountNotFoundException(String key,Object... arguments){super(key);this.arguments=arguments;} public Object[] getArguments(){return arguments;} }
