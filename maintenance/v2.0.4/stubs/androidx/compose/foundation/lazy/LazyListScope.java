package androidx.compose.foundation.lazy;
import androidx.compose.runtime.Composer;
import kotlin.jvm.functions.Function3;
public interface LazyListScope {
    void item(Object key, Object type, Function3<Object, Composer, Integer, Object> content);
}
