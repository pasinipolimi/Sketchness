package models;

import com.feth.play.module.pa.user.AuthUser;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import play.db.ebean.Model;

@Entity
public class LinkedAccount extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	public Long id;

	@ManyToOne
	public User user;

	public String providerUserId;
	public String providerKey;

	public static final Finder<Long, LinkedAccount> find = new Finder<Long, LinkedAccount>(
			Long.class, LinkedAccount.class);

	public static LinkedAccount findByProviderKey(final User user, String key) {
                List<LinkedAccount> returned = find.where().eq("user", user).eq("providerKey", key).findList();
                if(returned.size()>0)
                    return returned.get(returned.size()-1);
                else
                    return null;
	}

	public static LinkedAccount create(final AuthUser authUser) {
		final LinkedAccount ret = new LinkedAccount();
		ret.update(authUser);
		return ret;
	}
	
	public void update(final AuthUser authUser) {
		this.providerKey = authUser.getProvider();
		this.providerUserId = authUser.getId();
	}

	public static LinkedAccount create(final LinkedAccount acc) {
		final LinkedAccount ret = new LinkedAccount();
		ret.providerKey = acc.providerKey;
		ret.providerUserId = acc.providerUserId;

		return ret;
	}
}