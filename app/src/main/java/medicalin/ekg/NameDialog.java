package medicalin.ekg;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class NameDialog extends AppCompatDialogFragment {

    private EditText editTextName;
    private NameDialogListener nameDialogListener;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_name_dialog, null);
        builder.setView(view)
                .setTitle("File Name")
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String namefile = editTextName.getText().toString();
                        nameDialogListener.applyText(namefile);
                    }
                });
        editTextName = view.findViewById(R.id.edit_filename);
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            nameDialogListener = (NameDialogListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString()+
            "Must implement NameDialogListener");
        }
    }

    public interface NameDialogListener{
        void applyText(String namefile);
    }
}
