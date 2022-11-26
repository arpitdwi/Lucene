package com.tutorialspoint.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter; 
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.demo.knn.DemoEmbeddings;
import org.apache.lucene.demo.knn.KnnVectorDict;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

/** Simple command-line based search demo. */
public class SearchFiles {

  private SearchFiles() {}

  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
	    String usage =
	        "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage] [-knn_vector knnHits]\n\nSee http://lucene.apache.org/core/9_0_0/demo/ for details.";
	    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
	      System.out.println(usage);
	      System.exit(0);
	    }

	    String index = "index/dd_cc";
	    String field = "contents";
	    String queries = "queries_refined/dd_cc/train";
	    int repeat = 0;
	    boolean raw = false;
	    int knnVectors = 0;
	    String queryString = null;
	    int hitsPerPage = 50;


	    DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
	    IndexSearcher searcher = new IndexSearcher(reader);
	    Analyzer analyzer = new StandardAnalyzer();
	    KnnVectorDict vectorDict = null;
	    if (knnVectors > 0) {
	      vectorDict = new KnnVectorDict(reader.directory(), IndexFiles.KNN_DICT);
	    }
		// Loop starts here 
		File dir = new File(queries);
		File[] directoryListing = dir.listFiles();
	    for (File child : directoryListing) {
	    
		BufferedReader in;
	    if (queries != null) {
	      in = Files.newBufferedReader(child.toPath(), StandardCharsets.UTF_8);
	    } else {
	      in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
	    }
	    QueryParser parser = new QueryParser(field, analyzer);
	    while (true) {


	      String line = queryString != null ? queryString : in.readLine();

	      if (line == null || line.length() == -1) {
	        break;
	      }

	      line = line.trim();
	      if (line.length() == 0) {
	        break;
	      }

	      Query query = parser.parse(line);
	      if (knnVectors > 0) {
	        query = addSemanticQuery(query, vectorDict, knnVectors);
	      }
	      //System.out.println("Searching for: " + query.toString(field));

	      if (repeat > 0) { // repeat & time as benchmark
	        Date start = new Date();
	        for (int i = 0; i < repeat; i++) {
	          searcher.search(query, 100);
	        }
	        Date end = new Date();
	        System.out.println("Time: " + (end.getTime() - start.getTime()) + "ms");
	      }

	      doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null,child.getName());

	      if (queryString != null) {
	        break;
	      }
	    }
	    if (vectorDict != null) {
	      vectorDict.close();
	    }
		}
	    reader.close();
	  }
	  
	  public static void doPagingSearch(
	      BufferedReader in,
	      IndexSearcher searcher,
	      Query query,
	      int hitsPerPage,
	      boolean raw,
	      boolean interactive,
	      String filename)
	      throws IOException {

	    // Collect enough docs to show 5 pages
	    TopDocs results = searcher.search(query, 5 * hitsPerPage);
	    ScoreDoc[] hits = results.scoreDocs;

	    int numTotalHits = Math.toIntExact(results.totalHits.value);
	    // System.out.println(numTotalHits + " total matching documents");
	    
	    int start = 0;
	    int end = Math.min(numTotalHits, hitsPerPage);
	    
	    FileWriter myWriter = new FileWriter("out/dd_cc/train/"+filename);
	    
	    
	    while (true) {

	      end = Math.min(hits.length, start + hitsPerPage);

	      for (int i = start; i < end; i++) {
	        if (raw) { // output raw format
	          System.out.println("doc=" + hits[i].doc + " score=" + hits[i].score);
	          continue;
	        }

	        Document doc = searcher.doc(hits[i].doc);
	        String path = doc.get("path");
	        if (path != null) {
	        	myWriter.write(path + "\n");
	          String title = doc.get("title");
	          if (title != null) {
	            System.out.println("   Title: " + doc.get("title"));
	          }
	        } else {
	          System.out.println((i + 1) + ". " + "No path for this document");
	        }
	      }

	      if (!interactive || end == 0) {
	        break;
	      }

	    }
	    myWriter.close();
	  }

  private static Query addSemanticQuery(Query query, KnnVectorDict vectorDict, int k)
      throws IOException {
    StringBuilder semanticQueryText = new StringBuilder();
    QueryFieldTermExtractor termExtractor = new QueryFieldTermExtractor("contents");
    query.visit(termExtractor);
    for (String term : termExtractor.terms) {
      semanticQueryText.append(term).append(' ');
    }
    if (semanticQueryText.length() > 0) {
      KnnVectorQuery knnQuery =
          new KnnVectorQuery(
              "contents-vector",
              new DemoEmbeddings(vectorDict).computeEmbedding(semanticQueryText.toString()),
              k);
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      builder.add(query, BooleanClause.Occur.SHOULD);
      builder.add(knnQuery, BooleanClause.Occur.SHOULD);
      return builder.build();
    }
    return query;
  }

  private static class QueryFieldTermExtractor extends QueryVisitor {
    private final String field;
    private final List<String> terms = new ArrayList<>();

    QueryFieldTermExtractor(String field) {
      this.field = field;
    }

    @Override
    public boolean acceptField(String field) {
      return field.equals(this.field);
    }

    @Override
    public void consumeTerms(Query query, Term... terms) {
      for (Term term : terms) {
        this.terms.add(term.text());
      }
    }

    @Override
    public QueryVisitor getSubVisitor(BooleanClause.Occur occur, Query parent) {
      if (occur == BooleanClause.Occur.MUST_NOT) {
        return QueryVisitor.EMPTY_VISITOR;
      }
      return this;
    }
  }
}