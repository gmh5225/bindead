package bindead.analyses.systems.natives;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javalx.data.Option;


public class FileSystemLoader implements IDefinitionLoader {
  private static final String FILE_EXT = ".rr";

  /** End with {@link File#separator} */
  private final List<String> basePaths = new ArrayList<String>();

  public FileSystemLoader (String... basePaths) {
    for (String basePath : basePaths) {
      if (!basePath.endsWith(File.separator)) {
        this.basePaths.add(basePath + File.separator);
      } else {
        this.basePaths.add(basePath);
      }
    }
  }

  @Override public boolean hasEntry (DefinitionId id) {
    for (String basePath : basePaths) {
      final File defFile = new File(filename(basePath, id));
      if (check(defFile)) {
        return true;
      }
    }
    return false;
  }

  @Override public Option<RawDefinition> getEntry (DefinitionId id) {
    for (String basePath : basePaths) {
      final File defFile = new File(filename(basePath, id));
      if (!check(defFile)) {
        continue;
      }

      final ByteBuffer rawBuffer = ByteBuffer.allocate((int) defFile.length());
      try {
        final FileInputStream fis = new FileInputStream(defFile);
        fis.getChannel().read(rawBuffer);
        rawBuffer.flip();
        fis.close();
        return Option.some(new RawDefinition(rawBuffer));
      } catch (FileNotFoundException err) {
        System.err.println("Cannot find native definition file '" + defFile.getAbsolutePath() + "':");
        err.printStackTrace();
      } catch (IOException err) {
        System.err.println("Error while reading native definition file:");
        err.printStackTrace();
      }
    }
    return Option.none();
  }

  private static boolean check (File file) {
    return file.exists() && !file.isDirectory();
  }

  private static String filename (String basePath, DefinitionId id) {
    return basePath + id.getBase() + FILE_EXT;
  }
}
