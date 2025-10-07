package org.mozilla.samples.compose.browser.browser

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

/**
 * Generic [ViewModel] wrapper for persisting different [State] objects between activity recreations.
 *
 * This allows easily instantiating a new [Store] with a [State] already persisted in a [ViewModel]
 * like it would happen after an activity recreation.
 *
 * @param owner [ViewModelStoreOwner] who's lifecycle will be used to persist the various [State]s.
 */
class StoreProvider<T> private constructor(
    owner: T,
) : ViewModel() where T: LifecycleOwner, T: ViewModelStoreOwner {
    /**
     * Map of the current [Store]s built by this [StoreProvider].
     * This is needed to be able to access and persist the latest [State] just before [owner] is destroyed.
     */
    @VisibleForTesting
    @PublishedApi
    internal val stores: MutableMap<Any, Store<*, *>> = mutableMapOf()

    /**
     * Map of the currently persisted [State]s.
     */
    @VisibleForTesting
    @PublishedApi
    internal val data: MutableMap<Any, State> = mutableMapOf()

    init {
        (owner as LifecycleOwner).lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                data.clear()
                stores.map { data[it.key] = it.value.state }
                stores.clear()
            }
        })
    }

    /**
     * Build a new [Store] with the provided [factory] and [initialState].
     * If the state already exists for the current [LifecycleOwner], it will be in subsequent [factory] calls.
     */
    inline fun <reified S: State, T : Store<*, *>> build(
        initialState: S,
        factory: (S) -> T,
    ) : T {
        val state = (data[S::class.java.name] as? S) ?: initialState
        return factory(state).also { stores[S::class.java.name] = it }
    }

    companion object {
        /**
         * Get a [StoreProvider] for the current [LifecycleOwner].
         */
        @Suppress("UNCHECKED_CAST")
        val <T> T.storeProvider : StoreProvider<T> where T: LifecycleOwner, T: ViewModelStoreOwner
            get() = ViewModelProvider(this, DataPersisterFactory(this))[StoreProvider::class.java] as StoreProvider<T>
    }

    @VisibleForTesting
    private class DataPersisterFactory<T>(
        private val owner : T
    ) : ViewModelProvider.Factory where T: LifecycleOwner, T: ViewModelStoreOwner {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            return StoreProvider(owner) as VM
        }
    }
}
