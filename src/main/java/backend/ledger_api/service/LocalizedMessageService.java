package backend.ledger_api.service;

import java.util.Locale;

public interface LocalizedMessageService {
    String getMessage(String key, Locale locale, Object... arguments);
}
