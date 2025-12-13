## 2024-05-23 - RecyclerView Adapter Listener Optimization
**Learning:** Moving `setOnClickListener` from `onBindViewHolder` to `onCreateViewHolder` significantly reduces object allocation during scrolling.
**Action:** Always set static listeners in `onCreateViewHolder` and use `bindingAdapterPosition` to retrieve the data item.
