require "csv"

#modules = ["ECore2GMF"]
modules = ["ECore2GMF","EcoreHelper","ECoreUtil","Formatting","ShortestPath"]

modules.each do |m|

	pass1 = File.read("#{m}_pass1.csv")
	pass2 = File.read("#{m}_pass2.csv")

	csv_pass1 = CSV.parse(pass1, :headers => true)
	csv_pass2 = CSV.parse(pass2, :headers => true)

	CSV.open("#{m}.csv", "wb") do |csv|
		csv << ["Mutation Operator","Gen.","Trivial","Killed","Live","Equiv.","Invalid"]
		csv_pass1.each do |row1|
			found = false
			csv_pass2.each do |row2|
				if(row1["Mutation Operator"] == row2["Mutation Operator"])
					found = true
					entry = [row1["Mutation Operator"], row1["Gen."], row1["Trivial"], row1["Killed"], row2["Live"], (row2["Equiv."].to_i + row2["Not Killed"].to_i).to_s, (row1["Invalid"].to_i + row2["Invalid"].to_i).to_s]
					csv<<entry
				end
			end
			if(not found)
				entry = [row1["Mutation Operator"], row1["Gen."], row1["Trivial"], row1["Killed"], 0.to_s, 0.to_s, row1["Invalid"]]
				csv<<entry
			end
		end
	end
end

