package com.newtonapp.view.adapter.rvadapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.newtonapp.R;
import com.newtonapp.model.rvmodel.ReportRvModel;

import java.util.ArrayList;

public class ReportRvAdapter extends BaseSingleViewTypeRvAdapter<ReportRvModel, ReportRvAdapter.ViewHolder> {

    public ReportRvAdapter(ArrayList<ReportRvModel> data, int itemLayoutRes) {
        super(data, itemLayoutRes);
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull ReportRvModel report) {
        holder.setReport(report);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        AppCompatTextView tvReportDate;
        AppCompatTextView tvReportIDPrinter;
        AppCompatTextView tvReportIDCustomer;
        AppCompatTextView tvReportStatus;
        ReportRvModel report;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportDate = itemView.findViewById(R.id.report_tv_date);
            tvReportIDPrinter = itemView.findViewById(R.id.report_tv_idprinter);
            tvReportIDCustomer = itemView.findViewById(R.id.report_tv_idcustomer);
            tvReportStatus = itemView.findViewById(R.id.report_tv_status);
        }

        private void setReport(ReportRvModel report) {
            this.report = report;
            updateView();
        }

        private void updateView() {
            tvReportDate.setText(report.getIdcustomer());
            tvReportIDPrinter.setText(report.getIdprinter());
            tvReportIDCustomer.setText(report.getIdcustomer());
            tvReportStatus.setText(report.getStatus());
            tvReportStatus.setTextColor(getStatusColor());
        }

        private int getStatusColor() {
            int statusColor = R.color.status_unknown;
            if (report.getStatus().equalsIgnoreCase(itemView.getContext().getString(R.string.status_solved)))
                statusColor = R.color.status_solved;
            else if (report.getStatus().equalsIgnoreCase(itemView.getContext().getString(R.string.status_hold)))
                statusColor = R.color.status_hold;
            return itemView.getContext().getResources().getColor(statusColor);
        }
    }
}