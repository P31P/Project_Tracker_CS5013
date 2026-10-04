package tracker.auth;

import java.util.Locale;
import org.springframework.stereotype.Service;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;

@Service
public class AllowlistService {

    private final AppUserRepository users;

    public AllowlistService(AppUserRepository users) {
        this.users = users;
    }

    public boolean isAllowed(String email) {
        if (email == null) {
            return false;
        }
        return users.findByEmail(email.toLowerCase(Locale.ROOT))
                .map(AppUser::isAllowlisted)
                .orElse(false);
    }
}
