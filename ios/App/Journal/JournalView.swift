import SwiftUI
import DataStore

/// Journal (PRD §13.6): local-only reflections, newest first, swipe to
/// delete. Journal text never leaves the device and never enters analytics.
struct JournalView: View {

    @EnvironmentObject private var model: AppModel
    @State private var entries: [JournalEntry] = []

    var body: some View {
        Group {
            if entries.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "book.closed")
                        .font(.system(size: 48))
                        .foregroundStyle(.secondary)
                    Text("journal_empty_title").font(.headline)
                    Text("journal_empty_body")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding()
            } else {
                List {
                    ForEach(entries, id: \.entryId) { entry in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(domainNameKey(entry.domain)).font(.caption.bold())
                                Spacer()
                                Text(formatted(entry.createdAt))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Text(entry.text)
                        }
                        .padding(.vertical, 4)
                    }
                    .onDelete(perform: delete)
                }
            }
        }
        .navigationTitle(Text("journal_title"))
        .onAppear(perform: reload)
    }

    private func reload() {
        entries = model.journalRepository.list()
    }

    private func delete(at offsets: IndexSet) {
        for index in offsets {
            try? model.journalRepository.delete(entryId: entries[index].entryId)
        }
        reload()
    }

    private func formatted(_ epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1000)
        return date.formatted(date: .abbreviated, time: .shortened)
    }

    private func domainNameKey(_ domain: String) -> LocalizedStringKey {
        switch domain {
        case "career": return "domain_career"
        case "wealth": return "domain_wealth"
        case "family": return "domain_family"
        case "health": return "domain_health"
        default: return LocalizedStringKey(domain)
        }
    }
}
