package co.chatsdk.core.handlers;

import co.chatsdk.core.dao.BThread;
import io.reactivex.Single;

/**
 * Created by SimonSmiley-Andrews on 01/05/2017.
 */

public interface PublicThreadHandler {

    /**
     * @brief Create a public group thread with a name. This can be used for group discussion
     */
    public Single<BThread> createPublicThreadWithName(final String name);
    public Single<BThread> createPublicThreadWithName(final String name, final String entityID);
}
