To run Lucene for REAT (https://arxiv.org/abs/1809.04276)
1) First run IndexFiles.java code. Index path is where the index is stored and docsPath consists of the files to index. 
2) Then in order to query run the SearchFiles code. queries contains the files to query. This will dump the topNHits file locations (in this case 50) for each file in the out folder. 
3) Note before running the queries need to be preprocessed(removal of special characters) using the lucene_preprocessing.ipynb
4) To get the actual response from the file locations run lucene_ref_generator.ipynb. This does some checks to ensure responses from the same dialogue aren't picked up as reference documents for the training dataset. It also checks whether the response is not exactly identical to the GT
5) Finally to combine all of them into a single txt file which is then used as the ref document run final_combiner.ipynb

Refer to sample file paths to get an understanding of the file formats.  