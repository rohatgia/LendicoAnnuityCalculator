import java.util.*;
import java.io.*;
import java.math.*;
import java.text.*;

public class PlanGenerator{
  public static ArrayList<HashMap<String,Float>> info = new ArrayList<HashMap<String,Float>>();
  public static int count = 0;
  public static void main (String[] args){
    Scanner in = new Scanner(System.in);
    System.out.println("Please enter the name of the file");
    String fileName = in.nextLine();
    File inFile = new File(fileName);
    
    try{
      BufferedReader payloadBR = new BufferedReader(new FileReader(inFile));
      HashMap<String,String> payloadHM = new HashMap<String, String>();
      
      String line;
      try{
        while((line = payloadBR.readLine()) != null){
          //System.out.println("DEBUG: " + line);
          if(!((line.equals("{"))||(line.equals("}")))){
            String[] splits = line.split("\":");
            //System.out.println("DEBUG2: " + splits[0] + " " + splits[1]);
            splits[0] = splits[0].substring(2,(splits[0].length()));
            if(splits[1].charAt(1)=='"'){
              splits[1] = splits[1].substring(2,(splits[1].length()-2));
            }
            else{
              splits[1] = splits[1].substring(0,(splits[1].length()-1));
            }
            splits[1] = splits[1].trim();
            //System.out.println("DEBUG3: " + splits[0] + " " + splits[1]);
            payloadHM.put(splits[0],splits[1]);
          }
        }
        
        float[] annuityValues = new float[3];
        String[] dateSplit = new String[3];
        Iterator it = payloadHM.entrySet().iterator();
        while(it.hasNext()){
          Map.Entry pair = (Map.Entry)it.next();
          //System.out.println(pair);
          String key = (String)pair.getKey();
          String value = (String)pair.getValue();
          switch(key){
            case "loanAmount":
              annuityValues[0] = Float.parseFloat(value);
              break;
            case "nominalRate":
              annuityValues[1] = Float.parseFloat(value);
              break;
            case "duration":
              annuityValues[2] = Float.parseFloat(value);
              break;
            case "startDate":
              dateSplit = value.split(":");
              break;
            default:
              //
              break;
          }
        }
        
        float annuity = annuity(annuityValues[1],(int)annuityValues[2],annuityValues[0]);        
        float interest = interest(annuityValues[1],30,annuityValues[0],360);       
        float principal = principal(annuity, interest);       
        float remainder = remainder(annuityValues[0],principal);
        annuity = round(annuity,2);
        interest = round(interest,2);
        principal = round(principal,2);
        remainder = round(remainder,2);
        HashMap<String, Float> values = new HashMap<String, Float>();
        values.put("borrowerPaymentAmount",annuity);
        values.put("interest",interest);
        values.put("initialOutstandingPrincipal",annuityValues[0]);
        values.put("principal",principal);
        values.put("remainingOutstandingPrincipal",remainder);
        info.add(values);
        for(int i = 1;i<annuityValues[2];i++){
          values(annuityValues);
        }
        
        String[] dates = dateCreator(dateSplit,annuityValues[2]);
        
        BufferedWriter writer = new BufferedWriter(new FileWriter("annuity.json"));
        writer.write("{\n");
        writer.write("  [\n");
        int i = 0;
        for(HashMap<String,Float> f:info){
          writer.write("    {\n");
          Iterator it1 = f.entrySet().iterator();
          while(it1.hasNext()){
            Map.Entry pair = (Map.Entry)it1.next();
            String key = (String)pair.getKey();
            float value = (float)pair.getValue();
            StringBuilder sb = new StringBuilder("");
            sb.append("\"");
            sb.append(key);
            sb.append("\"");
            sb.append(": \"");
            sb.append(value);
            sb.append("\",\n");
            writer.write("      " + sb.toString());
          }
          writer.write("      " + dates[i++] + ",\n");
          writer.write("    },\n");
        }
        writer.write("  ]\n");
        writer.write("}");
        writer.close();
        
      }
      catch(IOException e){
        e.printStackTrace();
      }
    }
    catch(FileNotFoundException e){
      e.printStackTrace();
    }
  }
  
  public static float annuity(float nominalInterest, int duration, float loan){
    float monthlyInterest = nominalInterest/(12*100);
    return (loan*monthlyInterest)/(float)(1-Math.pow(1+monthlyInterest,-duration));
  }
  
  public static float interest(float nominalInterest, int daysMonth, float outstandingPrincipal, int daysYear){
    float interest = nominalInterest*daysMonth*outstandingPrincipal;
    interest = interest/daysYear;
    interest = interest/100;
    return interest;
  }
  
  public static float principal(float annunity, float interest){
    return annunity - interest;
  }
  
  public static float remainder(float outstandingPrincipal, float principal){
    return outstandingPrincipal - principal;
  }
  
  public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
  
  public static void values(float[] annuityValues){
    HashMap<String,Float> aval = info.get(count);
    Iterator it1 = aval.entrySet().iterator();
    float initial = 0;
    while(it1.hasNext()){
      Map.Entry pair = (Map.Entry)it1.next();
      String key = (String)pair.getKey();
      float value = (float)pair.getValue();
      switch(key){
        case "remainingOutstandingPrincipal":
          initial = value;
          break;
        default:
          //
          break;
      }  
    }
    float annuity = annuity(annuityValues[1],(int)(annuityValues[2]-1-count),initial);
    float interest = interest(annuityValues[1],30,initial,360);
    float principal = principal(annuity, interest);
    float remainder = remainder(initial,principal);
    annuity = round(annuity,2);
    interest = round(interest,2);
    principal = round(principal,2);
    remainder = round(remainder,2);
    HashMap<String, Float> values = new HashMap<String, Float>();
    values.put("borrowerPaymentAmount",annuity);
    values.put("interest",interest);
    values.put("initialOutstandingPrincipal",initial);
    values.put("principal",principal);
    values.put("remainingOutstandingPrincipal",remainder);
    info.add(values);
    count++;
  }
  
  public static String[] dateCreator(String[] dateSplit, float duration){
    int dur = (int)duration;
    String[] dateCreate = new String[dur];
    String startDate = dateSplit[0];
    String sub1 = startDate.substring(0,10);
    String sub2 = startDate.substring(10,startDate.length());
    int year = Integer.parseInt(sub1.substring(0,4));
    int day = Integer.parseInt(sub1.substring(8,10));
    int month = Integer.parseInt(sub1.substring(5,7));
    for(int i = 0;i<dur;i++){
      StringBuilder sb = new StringBuilder("");
      sb.append("\"date\": \""); 
      int sum = year+(month/12);
      sb.append(sum + "-");
      if(month%12 < 10 && month%12 != 0){
        sb.append("0" + month%12 + "-");
      }
      else if (month%12 == 0){
        sb.append("12-");
      }
      else{
        sb.append(month%12 + "-");
      }
      sb.append(day + sub2 + ":" + dateSplit[1] + ":00Z\"");
      dateCreate[i] = sb.toString();
      month++;
    }
    return dateCreate;
  }
    
}
        