package backend.ledger_api.exception;

public class AccountRequestException extends RuntimeException { private final Object[] arguments; public AccountRequestException(String key,Object... arguments){super(key);this.arguments=arguments;} public Object[] getArguments(){return arguments;} }
