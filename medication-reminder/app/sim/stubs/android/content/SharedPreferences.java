package android.content; import java.util.*;
public class SharedPreferences { final Map<String,Object> m = new HashMap<>();
  public String getString(String k, String d) { Object v = m.get(k); return v == null ? d : (String) v; }
  @SuppressWarnings("unchecked") public Set<String> getStringSet(String k, Set<String> d) { Object v = m.get(k); return v == null ? d : (Set<String>) v; }
  public Editor edit() { return new Editor(); }
  public class Editor { Map<String,Object> p = new HashMap<>();
    public Editor putString(String k, String v) { p.put(k, v); return this; }
    public Editor putStringSet(String k, Set<String> v) { p.put(k, new HashSet<>(v)); return this; }
    public boolean commit() { m.putAll(p); return true; } public void apply() { commit(); } } }
