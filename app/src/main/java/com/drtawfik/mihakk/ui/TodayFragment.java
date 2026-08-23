package com.drtawfik.mihakk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;

import java.util.List;

/** The screen that answers "what needs me today?" and nothing else. */
public class TodayFragment extends Fragment {

    private LinearLayout chips;
    private LinearLayout sections;
    private TextView empty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_today, parent, false);
        chips = v.findViewById(R.id.chips);
        sections = v.findViewById(R.id.sections);
        empty = v.findViewById(R.id.empty);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        Repo repo = new Repo(requireContext());
        Repo.Board board = repo.board();

        chips.removeAllViews();
        chips.addView(Ui.chip(requireContext(),
                getString(R.string.chip_this_year, repo.countDoneThisYear())));
        chips.addView(Ui.chip(requireContext(),
                getString(R.string.chip_verified, repo.countVerified())));
        chips.addView(Ui.chip(requireContext(),
                getString(R.string.chip_total, repo.count(Review.DONE_SQL))));

        sections.removeAllViews();
        addSection(getString(R.string.sec_overdue), board.overdue);
        addSection(getString(R.string.sec_due_soon), board.dueSoon);
        addSection(getString(R.string.sec_invitations), board.undecided);
        addSection(getString(R.string.sec_later), board.later);

        empty.setVisibility(board.openCount == 0 ? View.VISIBLE : View.GONE);
    }

    private void addSection(String title, List<Review> items) {
        if (items.isEmpty()) return;
        sections.addView(Ui.sectionHeader(requireContext(), title));
        for (Review r : items) {
            View card = ReviewRow.inflate(requireContext(), sections);
            ReviewRow.bind(card, r);
            card.setOnClickListener(v -> open(r));
            sections.addView(card);
        }
    }

    private void open(Review r) {
        Intent i = new Intent(requireContext(), ReviewEditActivity.class);
        i.putExtra(ReviewEditActivity.EXTRA_ID, r.id);
        startActivity(i);
    }
}
