import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class export {
  public static void main(String[] args) throws Exception {
    Path path = Paths.get("index");
    Directory index = FSDirectory.open(path);
    DirectoryReader dreader = DirectoryReader.open(index);
    IndexReader reader = DirectoryReader.open(index);
    StoredFields storedFields = reader.storedFields();

    TreeSet<String> groupIds = new TreeSet<String>();

    int num = reader.numDocs();
    for (int i = 0; i < num; i++)
    {
        Document d = storedFields.document(i);
        if (d.get("u") != null) {
          groupIds.add(d.get("u").split("\\|")[0] + ".*");
        }
    }

    BufferedWriter out = new BufferedWriter(new FileWriter("maven.default.index"));
    Iterator it = groupIds.iterator();
    while(it.hasNext()) {
      out.write(it.next().toString());
      out.newLine();
    }

    out.close();
    reader.close();
  }
}
