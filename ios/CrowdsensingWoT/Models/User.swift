import Foundation

/// Dati utente restituiti dall'azione `retrieveUserData` della dashboard
/// (equivalente di `User.java`).
struct User: Equatable {
    let email: String
    let points: Int
    let completedCampaigns: [String]

    init?(dictionary: [String: Any]) {
        guard let email = dictionary["email"] as? String else { return nil }
        self.email = email
        self.points = (dictionary["points"] as? NSNumber)?.intValue ?? 0
        self.completedCampaigns = dictionary["completed_campaigns"] as? [String] ?? []
    }
}
