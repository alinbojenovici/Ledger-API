package backend.ledger_api.service.api;

import backend.ledger_api.service.LocalizedMessageService;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.stereotype.Service;

@Service
public class LocalizedMessageServiceImplementation implements LocalizedMessageService {
    @Override
    public String getMessage(String key, Locale locale, Object... arguments) {
        String language = locale == null ? "en" : locale.getLanguage();
        String base = "ro".equalsIgnoreCase(language) ? "bundle.ro_messages" : "bundle.en_messages";
        ResourceBundle messages = ResourceBundle.getBundle(base, Locale.ROOT);
        if (!messages.containsKey(key))
         messages = ResourceBundle.getBundle("bundle.en_messages", Locale.ROOT);

        String pattern = messages.containsKey(key) ? messages.getString(key) : key;
        return new MessageFormat(pattern, locale == null ? Locale.ENGLISH : locale).format(arguments);
    }
}
