import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Normalize {

    public static class NormalizeMapper extends Mapper<LongWritable, Text, Text, Text> {

        // map method
        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            //movieA:movieB \t relation
            //collect the relationship list for movieA
            String[] line = value.toString().split("\t");
            String[] pair = line[0].split(":");
            context.write(new Text(pair[0]), new Text(pair[1] + ":" + line[1]));
        }
    }

    public static class NormalizeReducer extends Reducer<Text, Text, Text, Text> {
        // reduce method
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            //key = movieA, value=<movieB:relation, movieC:relation...>
            //normalize each unit of co-occurrence matrix
            Map<String, Double> map = new HashMap<String, Double>();
            int count = 0;
            for (Text value : values) {
                String[] unit = value.toString().split(":");
                count += Integer.parseInt(unit[1]);
                Double unitValue = (double) Integer.parseInt(unit[1]);
                map.put(unit[0], unitValue);
            }
            for (String mapKey : map.keySet()) {
                context.write(new Text(mapKey), new Text(key + "=" + String.valueOf(map.get(mapKey) / count)));
            }
        }
    }

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf);
        job.setMapperClass(NormalizeMapper.class);
        job.setReducerClass(NormalizeReducer.class);

        job.setJarByClass(Normalize.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        TextInputFormat.setInputPaths(job, new Path(args[0]));
        TextOutputFormat.setOutputPath(job, new Path(args[1]));

        job.waitForCompletion(true);
    }
}
