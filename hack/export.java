import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Export {
  public static void main(String[] args) throws Exception {
    Path path = Paths.get("index");
    Directory index = FSDirectory.open(path);
    IndexReader reader = DirectoryReader.open(index);

    int num = reader.numDocs();
    for ( int i = 0; i < num; i++)
    {
        Document d = reader.document( i);
        System.out.println( "d=" +d);
    }
    reader.close();
  }
}
