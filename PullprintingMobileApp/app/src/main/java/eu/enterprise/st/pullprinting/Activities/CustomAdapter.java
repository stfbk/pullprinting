package eu.enterprise.st.pullprinting.Activities;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import eu.enterprise.st.pullprinting.Model.Model;
import eu.enterprise.st.pullprinting.R;

import java.util.ArrayList;


public class CustomAdapter extends BaseAdapter {
    private Context context;
    public static ArrayList<Model> modelArrayList;
    public ArrayList <String> job_selezionati=new ArrayList<String>();
    public CustomAdapter(Context context, ArrayList<Model> modelArrayList) {
        this.context = context;
        this.modelArrayList = modelArrayList;

        TokenActivity.secure_print_active = false;
        for (Model element:modelArrayList) {
            if(element.getSelected()==true)
            {
                try {
                if (element.getJob().split(":_:")[2] != null || element.getJob().split(":_:")[2].equals("\t*SECURE*")) {
                    securePrintDocument_number += 1;
                    if (securePrintDocument_number > 0) {
                        TokenActivity.secure_print_active = true;
                    } else {
                        TokenActivity.secure_print_active = false;
                    }
                }
                }
                catch (Exception e){
                    Log.d("Error", "Exception occurred: "+e);
                }
                String job_splittato = element.getJob().split(":_:")[1];
                job_selezionati.add(job_splittato);

            }
        }

    }

    boolean securePrintDocument = false;
    int securePrintDocument_number = 0;

    @Override
    public int getViewTypeCount() {
        return getCount();
    }
    @Override
    public int getItemViewType(int position) {
        return position;
    }
    @Override
    public int getCount() {
        return modelArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return modelArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder(); LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.lv_item, null, true);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.cb);
            holder.tvJob = (TextView) convertView.findViewById(R.id.job);
            convertView.setTag(holder);
        }else {
            // the getTag returns the viewHolder object set as a tag to the view
            holder = (ViewHolder)convertView.getTag();
        }

        holder.checkBox.setText("");
        holder.tvJob.setText(modelArrayList.get(position).getJob());
        //System.out.println("modelArrayList.get(position).getJob"+modelArrayList.get(position).getJob());
        holder.checkBox.setChecked(modelArrayList.get(position).getSelected());
        holder.checkBox.setTag(R.integer.btnplusview, convertView);
        holder.checkBox.setTag( position);
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                View tempview = (View) holder.checkBox.getTag(R.integer.btnplusview);
                TextView tv = (TextView) tempview.findViewById(R.id.job);
                Integer pos = (Integer)  holder.checkBox.getTag();

                if(modelArrayList.get(pos).getSelected()){
                    modelArrayList.get(pos).setSelected(false);

                    try {
                        if (modelArrayList.get(pos).getJob().split(":_:")[2] != null || modelArrayList.get(pos).getJob().split(":_:")[2].equals("\t*SECURE*")) {
                            //System.out.println("SECURE DOCUMENT SELECTED");
                            securePrintDocument_number -= 1;
                            //System.out.println("securePrintDocument_number: "+securePrintDocument_number);
                            if(securePrintDocument_number>0) {
                                TokenActivity.secure_print_active=true;
                            }
                            else {
                                TokenActivity.secure_print_active=false;
                            }
                        }
                    }
                    catch (Exception e){
                        Log.d("Error", "Exception occurred: "+e);
                    }

                    String job_splittato = modelArrayList.get(pos).getJob().split(":_:")[1];
                    job_selezionati.remove(job_splittato);

                }else {
                    modelArrayList.get(pos).setSelected(true);
                    String job_splittato = modelArrayList.get(pos).getJob().split(":_:")[1];
                    try {
                        if (modelArrayList.get(pos).getJob().split(":_:")[2] != null || modelArrayList.get(pos).getJob().split(":_:")[2].equals("\t*SECURE*")) {
                            securePrintDocument = true;
                            securePrintDocument_number += 1;
                            //System.out.println("securePrintDocument_number: "+securePrintDocument_number);
                            if(securePrintDocument_number>0) {
                                TokenActivity.secure_print_active=true;
                            }
                            else {
                                TokenActivity.secure_print_active=false;
                            }
                        }
                    }
                    catch (Exception e){
                        Log.d("Error", "Exception occurred: "+e);
                    }
                    job_selezionati.add(job_splittato);
                }
            }
        });
        return convertView;
    }

    private class ViewHolder {
        protected CheckBox checkBox;
        private TextView tvJob;

    }

}