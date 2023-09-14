package estudo.alura.screenmatch.principal;

import estudo.alura.screenmatch.model.DadosEpisodio;
import estudo.alura.screenmatch.model.DadosSerie;
import estudo.alura.screenmatch.model.DadosTemporada;
import estudo.alura.screenmatch.model.Episodio;
import estudo.alura.screenmatch.service.ConsumoApi;
import estudo.alura.screenmatch.service.ConverteDados;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    Scanner scanner = new Scanner(System.in);
    ConsumoApi consumoApi = new ConsumoApi();
    ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";

    public void exibeMenu() {
        System.out.println("Digite o nome da série que quer listar");
        String nomeSerie = scanner.nextLine();

        String enderecoSerie = "https://www.omdbapi.com/?t=" + nomeSerie.replace(" ", "+") + API_KEY;
        String json = consumoApi.obterDados(enderecoSerie);
        System.out.println(json);

        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        System.out.println(dados);

        String enderecoEpisodio = "https://www.omdbapi.com/?t=" + nomeSerie.replace(" ", "+") + "&season=1&episode=2" + API_KEY;
        json = consumoApi.obterDados(enderecoEpisodio);
        DadosEpisodio dadosEpisodio = conversor.obterDados(json, DadosEpisodio.class);
        //  System.out.println(dadosEpisodio);

        List<DadosTemporada> temporadas = new ArrayList<>();
        for (int i = 1; i <= dados.totalTemporadas(); i++) {
            String enderecoTemporada = "https://www.omdbapi.com/?t=" + nomeSerie.replace(" ", "+") + "&season=" + i + API_KEY;
            json = consumoApi.obterDados(enderecoTemporada);
            DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
            temporadas.add(dadosTemporada);
        }


        List<DadosEpisodio> dadosEpisodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream())
                .collect(Collectors.toList());
        System.out.println("\n Episódios sem formatação");
        dadosEpisodios.forEach(System.out::println);

        //Algumas avaliações estavam N/A, então eu faço um filter para filtrar e dps rankear em ordem decrescente
        System.out.println("\nTop 5 episódios");
        dadosEpisodios.stream().filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
                .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
                .limit(5)
                .forEach(System.out::println);

        // Classe de episódio com dados mais detalhados
        //O segundo .map cria um novo episódio a cada episódio de temporada que passa, para inserir o número da temporada a qual pertence o episódio
        List<Episodio> episodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream()
                        .map(d -> new Episodio(t.numero(), d))
                ).collect(Collectors.toList());

        System.out.println("\nEpisódios formatados");
        episodios.forEach(System.out::println);

        System.out.println("\nTop 10 episódios formatados com temporada");
        episodios.stream()
                .sorted(Comparator.comparing(Episodio::getAvaliacao).reversed())
                .peek(e -> System.out.println("Ordenação reversa " + e))
                .limit(10)
                .peek(e -> System.out.println("Limitação " + e))
                .map(e -> e.getTitulo().toUpperCase())
                .peek(e -> System.out.println("Mapeamento " + e))
                .forEach(System.out::println);

        System.out.println("A partir de que ano você quer ver os episódios?");
        Integer ano = scanner.nextInt();
        scanner.nextLine();


        //Personalizando o sout e listando por datas
        LocalDate dataBusca = LocalDate.of(ano, 1, 1);
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        episodios.stream()
                .filter(e -> e.getDataLancamento() != null && e.getDataLancamento().isAfter(dataBusca)).forEach(e -> System.out.println(
                        "Temporada: " + e.getTemporada() +
                                " Episódio: " + e.getTitulo() +
                                " Data lançamento: " + e.getDataLancamento()
                                .format(formatador)));


        //findAny é paralelismo, mais rapido mas menos preciso
        //System.out.println("\nDigite um título ");
        //String tituloDigitado = scanner.nextLine();
        //Optional<Episodio> episodioEncontrado = episodios.stream()
        //      .filter(e -> e.getTitulo().toUpperCase().contains(tituloDigitado.toUpperCase()))
        //      .findFirst();
        // if(episodioEncontrado.isPresent()){
        //  System.out.printf("Episódio encontrado! %s" , episodioEncontrado.get());
        // } else {
        // System.out.println("Episódio não encontrado");
        //}

        // Mapeando a média de avaliações de acordo com a
        Map<Integer, Double> avaliacoesPorTemporada = episodios.stream()
                .filter(e -> e.getAvaliacao() > 0.0)
                .collect(Collectors.groupingBy(Episodio::getTemporada, Collectors.averagingDouble(Episodio::getAvaliacao)));
        System.out.println(avaliacoesPorTemporada);

        //Gera estatísticas básicas, como soma, média, minino e maximo
        DoubleSummaryStatistics est = episodios.stream()
                .filter(e -> e.getAvaliacao() > 0.0)
                .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));
        System.out.println(est.getMax());

    }

}

